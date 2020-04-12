import sbt._

object Dependencies {
  lazy val scalaTest  = "org.scalatest" %% "scalatest"   % "3.0.5"
  lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

  lazy val cats       = "org.typelevel" %% "cats-core"   % "2.0.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.2"

  lazy val fs2        = "co.fs2" %% "fs2-core" % "2.2.1"
  lazy val fs2IO      = "co.fs2" %% "fs2-io"   % "2.2.1"

  lazy val raspberryPi = "com.pi4j" % "pi4j-core" % "1.1"

  lazy val zioVersion = "1.0.0-RC18-2"
  lazy val zio        = "dev.zio" %% "zio"               % zioVersion
  lazy val zioStreams = "dev.zio" %% "zio-streams"       % zioVersion
  lazy val zioCats    = "dev.zio" %% "zio-interop-cats"  % "2.0.0.0-RC12"
  lazy val zioLogging = "dev.zio" %% "zio-logging-slf4j" % "0.2.6"

  lazy val http4sVersion = "0.21.3"

  lazy val http4sBlazeServer = "org.http4s"      %% "http4s-blaze-server" % http4sVersion
  lazy val http4sCirce       = "org.http4s"      %% "http4s-circe"        % http4sVersion
  lazy val http4sDsl         = "org.http4s"      %% "http4s-dsl"          % http4sVersion

  lazy val circeVersion = "0.12.3"
  lazy val circe        = "io.circe" %% "circe-core"    % circeVersion
  lazy val circeParser  = "io.circe" %% "circe-parser"  % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion

}
