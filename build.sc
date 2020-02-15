import coursier.core.Version
import mill._
import mill.define.Target
import mill.scalalib._
import os.Path

val scalaVersions = Seq(
  //  "2.10.7",
  "2.11.12",
  "2.12.10",
  "2.13.1",
)

object lib extends Cross[LibModule](scalaVersions: _*) {
  def updateReadme: T[Path] = T {
    this(scalaVersions.last).mdoc()
    val readme = os.pwd / "README.md"
    os.copy(this(scalaVersions.last).mdocTargetDirectory() / "README.md", readme, replaceExisting = true)
    readme
  }

}

class LibModule(val crossScalaVersion: String) extends CrossSbtModule with MdocModule {
  override def mdocVersion = "2.1.1"

  override def scalacOptions = Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xlint",
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused",
    "-Ywarn-value-discard",               // Warn when non-Unit expression results are unused.
  ) ++ VersionDependent.Split[Seq[String]](
    oldValue = Seq(
    ),
    fromVersion = "2.12",
    newValue = Seq(
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    ),
  )(crossScalaVersion) ++ VersionDependent.Split[Seq[String]](
    oldValue = Seq(
      "-Xfuture",                          // Turn on future language features.
      "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
      "-Ypartial-unification",             // Enable partial unification in type constructor inference
      "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
      "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
      "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
      "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    ),
    fromVersion = "2.13",
    newValue = Seq(),
  )(crossScalaVersion)
}

sealed abstract class VersionDependent[A](val apply: Version => A) {
  final def apply(scalaVersion: String): A = apply(Version(scalaVersion))
}

object VersionDependent {
  implicit def apply[A](value: A): VersionDependent[A] = Constant(value)

  case class Constant[A](value: A) extends VersionDependent[A](_ => value)

  case class Split[A](oldValue: VersionDependent[A], fromVersion: Version, newValue: VersionDependent[A]) extends
    VersionDependent[A](v => (if (v < fromVersion) oldValue else newValue)(v))

  object Split {
    def apply[A](oldValue: VersionDependent[A], fromVersion: String, newValue: VersionDependent[A]): VersionDependent[A] =
      Split(oldValue, Version(fromVersion), newValue)
  }
}

// Based on https://github.com/lihaoyi/mill/blob/master/contrib/tut/src/TutModule.scala
// TODO upstream PR
import coursier.MavenRepository
import mill.scalalib._
import scala.util.matching.Regex

/**
 * Mdoc is a documentation tool which compiles and evaluates Scala code in documentation files and provides various options for configuring how the results will be displayed in the compiled documentation.
 *
 * Extending this trait declares a Scala module which compiles markdown, HTML and `.txt` files in the `mdoc` folder of the module with Mdoc.
 *
 * By default the resulting documents are simply placed in the Mill build output folder but they can be placed elsewhere by overriding the [[mill.contrib.mdoc.MdocModule#mdocTargetDirectory]] task.
 *
 * For example:
 *
 * {{{
 * // build.sc
 * import mill._, scalalib._, contrib.mdoc.__
 *
 * object example extends MdocModule {
 *   def scalaVersion = "2.12.6"
 *   def mdocVersion = "0.6.7"
 * }
 * }}}
 *
 * This defines a project with the following layout:
 *
 * {{{
 * build.sc
 * example/
 *     src/
 *     mdoc/
 *     resources/
 * }}}
 *
 * In order to compile documentation we can execute the `mdoc` task in the module:
 *
 * {{{
 * sh> mill example.mdoc
 * }}}
 */
trait MdocModule extends ScalaModule {
  /**
   * This task determines where documentation files must be placed in order to be compiled with Mdoc. By default this is the `mdoc` folder at the root of the module.
   */
  def mdocSourceDirectory = T.sources { millSourcePath / 'docs }

  /**
   * A task which determines where the compiled documentation files will be placed. By default this is simply the Mill build's output folder for this task,
   * but this can be reconfigured so that documentation goes to the root of the module (e.g. `millSourcePath`) or to a dedicated folder (e.g. `millSourcePath / 'docs`)
   */
  def mdocTargetDirectory: T[os.Path] = T { T.dest }

  /**
   * A task which determines what classpath is used when compiling documentation. By default this is configured to use the same inputs as the [[mill.contrib.mdoc.MdocModule#runClasspath]],
   * except for using [[mill.contrib.mdoc.MdocModule#mdocIvyDeps]] rather than the module's [[mill.contrib.mdoc.MdocModule#runIvyDeps]].
   */
  def mdocClasspath: T[Agg[PathRef]] = T {
    // Same as runClasspath but with mdoc added to ivyDeps from the start
    // This prevents duplicate, differently versioned copies of scala-library ending up on the classpath which can happen when resolving separately
    transitiveLocalClasspath() ++
      resources() ++
      localClasspath() ++
      unmanagedClasspath() ++
      mdocIvyDeps()
  }

  /**
   * A task which determines the scalac plugins which will be used when compiling code examples with Mdoc. The default is to use the [[mill.contrib.mdoc.MdocModule#scalacPluginIvyDeps]] for the module.
   */
  def mdocScalacPluginIvyDeps: T[Agg[Dep]] = scalacPluginIvyDeps()

  /**
   * A [[scala.util.matching.Regex]] task which will be used to determine which files should be compiled with mdoc. The default pattern is as follows: `.*\.(md|markdown|txt|htm|html)`.
   */
  def mdocNameFilter: T[Regex] = T { """.*\.(md|markdown|txt|htm|html)""".r }

  /**
   * The scalac options which will be used when compiling code examples with Mdoc. The default is to use the [[mill.contrib.mdoc.MdocModule#scalacOptions]] for the module,
   * but filtering out options which are problematic in the REPL, e.g. `-Xfatal-warnings`, `-Ywarn-unused-imports`.
   */
  def mdocScalacOptions: T[Seq[String]] =
    scalacOptions().filterNot(Set(
      "-Ywarn-unused:imports",
      "-Ywarn-unused-import",
      "-Ywarn-dead-code",
      "-Xfatal-warnings"
    ))

  /**
   * The version of Mdoc to use.
   */
  def mdocVersion: T[String]

  /**
   * A task which determines how to fetch the Mdoc jar file and all of the dependencies required to compile documentation for the module and returns the resulting files.
   */
  def mdocIvyDeps: T[Agg[PathRef]] = T {
    Lib.resolveDependencies(
      repositories :+ MavenRepository(s"https://dl.bintray.com/tpolecat/maven"),
      Lib.depToDependency(_, scalaVersion()),
      compileIvyDeps() ++ transitiveIvyDeps() ++ Seq(
        ivy"org.scalameta::mdoc:${mdocVersion()}"
      )
    )
  }

  /**
   * A task which performs the dependency resolution for the scalac plugins to be used with Mdoc.
   */
  def mdocPluginJars: T[Agg[PathRef]] = resolveDeps(mdocScalacPluginIvyDeps)()

  /**
   * Run Mdoc using the configuration specified in this module. The working directory used is the [[mill.contrib.mdoc.MdocModule#millSourcePath]].
   */
  def mdoc: T[os.CommandResult] = T {
    val in = mdocSourceDirectory().head.path.toIO.getAbsolutePath
    val out = mdocTargetDirectory().toIO.getAbsolutePath
    val re = mdocNameFilter()
    val opts = mdocScalacOptions()
    val pOpts = mdocPluginJars().map(pathRef => "-Xplugin:" + pathRef.path.toIO.getAbsolutePath)
    val mdocArgs = List(
      "--in", in,
      "--out", out,
//      "--include", re.pattern.toString,
      "--scalac-options", opts.mkString(" ")
    ) ++ pOpts
    println(mdocArgs)
    os.proc(
      'java,
      "-cp", mdocClasspath().map(_.path.toIO.getAbsolutePath).mkString(java.io.File.pathSeparator),
      "mdoc.Main",
      mdocArgs
    ).call(millSourcePath)
  }
}
