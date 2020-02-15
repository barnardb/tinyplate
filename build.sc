import coursier.core.Version
import mill._
import mill.scalalib._

object lib extends Cross[LibModule](
  "2.11.12",
  "2.12.10",
  "2.13.1",
)

class LibModule(val crossScalaVersion: String) extends CrossSbtModule {

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
