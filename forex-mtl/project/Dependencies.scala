import sbt._

object Dependencies {

  object Versions {
    val cats       = "2.9.0"
    val catsEffect = "3.4.11"
    val fs2        = "3.7.0"
    val http4s     = "0.23.18"
    val circe      = "0.14.3" /* Do not upgrade more than 0.14.3 */

    val newType       = "0.4.4"
    val pureConfig    = "0.17.4"
    val ip4s          = "3.3.0"
    val kindProjector = "0.13.2"
    val logback       = "1.4.7"
    val log4cats      = "2.5.0"
    val scaffeine     = "5.3.0"

    val munit           = "0.7.29"
    val munitCatsEffect = "1.0.7"
    val scalaCheck      = "1.17.0"
    val catsScalaCheck  = "0.3.2"
    val catsEffectTest  = "1.5.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    // Core FP
    lazy val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val fs2        = "co.fs2"        %% "fs2-core"    % Versions.fs2

    // Http 4s
    lazy val http4sDsl    = http4s("http4s-dsl")
    lazy val http4sServer = http4s("http4s-ember-server")
    lazy val http4sClient = http4s("http4s-ember-client")
    lazy val http4sCirce  = http4s("http4s-circe")

    // Circe
    lazy val circeCore       = circe("circe-core")
    lazy val circeGeneric    = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser     = circe("circe-parser")

    // Newtype
    lazy val newType = "io.estatico" %% "newtype" % Versions.newType

    // Config
    lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig

    // Network
    lazy val ip4s = "com.comcast" %% "ip4s-core" % Versions.ip4s

    // Logging
    lazy val logback  = "ch.qos.logback" % "logback-classic" % Versions.logback
    lazy val log4cats = "org.typelevel" %% "log4cats-slf4j"  % Versions.log4cats

    // Cache
    lazy val scaffeine = "com.github.blemale" %% "scaffeine" % Versions.scaffeine

    // Compiler plugins
    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full

    // Testing
    lazy val munit           = "org.scalameta"     %% "munit"               % Versions.munit
    lazy val munitCatsEffect = "org.typelevel"     %% "munit-cats-effect-3" % Versions.munitCatsEffect
    lazy val scalaCheck      = "org.scalacheck"    %% "scalacheck"          % Versions.scalaCheck
    lazy val catsScalaCheck  = "io.chrisdavenport" %% "cats-scalacheck"     % Versions.catsScalaCheck
  }

}
