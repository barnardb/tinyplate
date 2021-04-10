import coursier.core.Version
import mill._
import mill.scalalib._
import mill.scalalib.publish._
import os.Path

import scala.reflect.runtime.universe

val scalaVersions = Seq(
  //  "2.10.7",
  "2.11.12",
  "2.12.13",
  "2.13.5",
  "3.0.0-RC2",
)

object lib extends Cross[LibModule](scalaVersions: _*) {
  def updateReadme: T[Path] = T {
    this(scalaVersions.last).mdoc()
    val readme = os.pwd / "README.md"
    os.copy(this(scalaVersions.last).mdocTargetDirectory() / "README.md", readme, replaceExisting = true)
    readme
  }
}

class LibModule(val crossScalaVersion: String) extends CrossSbtModule with MdocModule with PublishModule {
  override def artifactName: T[String] = "tinyplate"

  import ScalacOptions._
  override def scalacOptions: T[Seq[String]] = forScalaVersion(crossScalaVersion)(
    `-deprecation`,
    `-encoding`("utf-8"),
    `-explain-types`, `-explaintypes`,
    `-feature`,
    `-language:existentials`,
    `-language:experimental.macros`,
    `-language:higherKinds`,
    `-language:implicitConversions`,
    `-unchecked`,
    `-Xfatal-warnings`,
    `-Xfuture`,
    `-Xlint`,
    `-Ycheck-all-patmat`,
    `-Yno-adapted-args`,
    `-Ypartial-unification`,
    `-Ysafe-init`, `-Xcheckinit`,
    `-Ywarn-dead-code`,
    `-Ywarn-extra-implicit`,
    `-Ywarn-inaccessible`,
    `-Ywarn-infer-any`,
    `-Ywarn-nullary-override`,
    `-Ywarn-nullary-unit`,
    `-Ywarn-numeric-widen`,
    `-Ywarn-unused`,
    `-Ywarn-value-discard`,
  )

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest::3.2.7"
    )
    override def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  override def mdocVersion = "2.2.0"

  override def pomSettings = PomSettings(
    description = "A tiny Scala template engine",
    organization = "io.github.barnardb",
    url = "https://github.com/barnardb/tinyplate",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("barnardb", "tinyplate"),
    developers = Seq(
      Developer("barnardb", "Ben Barnard", "https://github.com/barnardb")
    )
  )

  override def publishVersion = "0.6.0"
}



object ScalacOptions {
  def forScalaVersion(version: String)(candidates: ScalacOption*): Seq[String] =
    forScalaVersion(Version(version))(candidates: _*)

  def forScalaVersion(version: Version)(candidates: ScalacOption*): Seq[String] =
    candidates.flatMap(_ (version))

  private object PertinentScalaVersions {
    val v2_11 = Version("2.11")
    val v2_12 = Version("2.12")
    val v2_13 = Version("2.13")
    val v3 = Version("3.0.0-RC2")
  }
  import PertinentScalaVersions._

  case object `-deprecation` extends ScalacOption("Emit warning and location for usages of deprecated APIs.")

  case class `-encoding`(encoding: String) extends ScalacOption("Specify character encoding used by source files.", argument = Some(encoding))

  case object `-explain-types` extends ScalacOption("Explain type errors in more detail.", v3)
  case object `-explaintypes` extends ScalacOption("Explain type errors in more detail.", until = v3)

  case object `-feature` extends ScalacOption("Emit warning and location for usages of features that should be imported explicitly.")

  case object `-language:existentials` extends ScalacOption("Existential types (besides wildcard types) can be written and inferred")

  case object `-language:experimental.macros` extends ScalacOption("Allow macro definition (besides implementation and application)")

  case object `-language:higherKinds` extends ScalacOption("Allow higher-kinded types")

  case object `-language:implicitConversions` extends ScalacOption("Allow definition of implicit functions called views")

  case object `-unchecked` extends ScalacOption("Enable additional warnings where generated code depends on assumptions.")

  case object `-Xfatal-warnings` extends ScalacOption("Fail the compilation if there are any warnings.")

  case object `-Xfuture` extends ScalacOption("Turn on future language features.", until = v2_13)

  case object `-Xlint` extends ScalacOption(???, until = v3) // TODO what's the Scala 3 equivalent?

  case object `-Ycheck-all-patmat` extends ScalacOption("Check exhaustivity and redundancy of all pattern matching (used for testing the algorithm).", v3)

  case object `-Yno-adapted-args` extends ScalacOption("Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.", until = v2_13)

  case object `-Ysafe-init` extends ScalacOption("Experimental support for Safe Initialization: https://dotty.epfl.ch/docs/reference/other-new-features/safe-initialization.html", v3)
  case object `-Xcheckinit` extends ScalacOption("Wrap field accessors to throw an exception on uninitialized access.", until = v3)

  case object `-Ypartial-unification` extends ScalacOption("Enable partial unification in type constructor inference", v2_11, v2_13)

  case object `-Ywarn-dead-code` extends ScalacOption("Warn when dead code is identified.", until = v3)

  case object `-Ywarn-extra-implicit` extends ScalacOption(???, v2_12, v3)

  case object `-Ywarn-inaccessible` extends ScalacOption("Warn about inaccessible types in method signatures.", until = v2_13)

  case object `-Ywarn-infer-any` extends ScalacOption("Warn when a type argument is inferred to be `Any`.", v2_11, v2_13)

  case object `-Ywarn-nullary-override` extends ScalacOption("Warn when non-nullary `def f()' overrides nullary `def f'.", until = v2_13)

  case object `-Ywarn-nullary-unit` extends ScalacOption("Warn when nullary methods return Unit.", until = v2_13)

  case object `-Ywarn-numeric-widen` extends ScalacOption("Warn when numerics are widened.", until = v3)

  case object `-Ywarn-unused` extends ScalacOption(???, v2_11, v3)

  case object `-Ywarn-value-discard` extends ScalacOption("Warn when non-Unit expression results are unused.", until = v3)
}

abstract class ScalacOption(description: => String, from: Version = null, until: Version = null, argument: Option[String] = None) {
  val asRawArguments: Seq[String] =
    universe.runtimeMirror(getClass.getClassLoader)
      .classSymbol(getClass)
      .name.decodedName.toString :: argument.toList
  val fromVersion: Option[Version] = Option(from)
  val untilVersion: Option[Version] = Option(until)

  def apply(scalaVersion: Version): Seq[String] =
    if (fromVersion.forall(_ <= scalaVersion) && untilVersion.forall(scalaVersion < _)) asRawArguments
    else Nil
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
    os.proc(
      'java,
      "-cp", mdocClasspath().map(_.path.toIO.getAbsolutePath).mkString(java.io.File.pathSeparator),
      "mdoc.Main",
      mdocArgs
    ).call(millSourcePath)
  }
}
