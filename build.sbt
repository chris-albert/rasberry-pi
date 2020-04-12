import Dependencies._
import com.typesafe.sbt.packager.docker.Cmd
import sbtrelease.ReleaseStateTransformations.{inquireVersions, runClean, runTest, setNextVersion, setReleaseVersion}

ThisBuild / organization := "io.lbert"
ThisBuild / scalaVersion := "2.12.10"

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    zio,
    zioStreams,
    zioLogging,
    scalaTest % Test,
    "org.apache.logging.log4j" % "log4j-api" % "2.11.1",
    "org.apache.logging.log4j" % "log4j-core" % "2.11.1"
  ),
  scalacOptions ++= Seq(
    "-encoding", "utf8", // Option and arguments on same line
    "-Xfatal-warnings",  // New lines for each options
    "-deprecation",
    "-unchecked",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps"
  ),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
)

lazy val `zio-raspberry-ws281x` = (project in file("zio-raspberry-ws281x"))
  .settings(
    commonSettings,
    name := "zio-raspberry-ws281x",
    libraryDependencies ++= Seq()
  ).enablePlugins(JavaAppPackaging, DockerPlugin)

lazy val server = (project in file("server"))
  .settings(
    commonSettings,
    name := "led-server",
    libraryDependencies ++= Seq(
      zioCats,
      http4sBlazeServer,
      http4sCirce,
      http4sDsl
    ),
    dockerUsername := Some("chrisalbert"),
    mainClass in (Compile, packageBin) := Some("io.lbert.rasberry.Main"),
    resolvers += Resolver.sonatypeRepo("releases"),
    releaseProcess := Seq[ReleaseStep](
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      ReleaseStep(releaseStepTask(publish in Docker)),
      setNextVersion
    ),
    dockerCommands := Seq(
      Cmd("FROM balenalib/raspberry-pi-openjdk:8-stretch"),
      Cmd("WORKDIR /opt/docker"),
      Cmd("RUN", "apt-get update && apt-get install wiringpi"),
      Cmd("ADD --chown=daemon:daemon opt /opt"),
      Cmd("USER daemon"),
      Cmd("ENTRYPOINT [\"/opt/docker/bin/rasberry-pi\"]"),
      Cmd("CMD []")
    )
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .dependsOn(`zio-raspberry-ws281x` % "compile->compile;test->test")

lazy val root = (project in file("."))
  .dependsOn(`zio-raspberry-ws281x` % "compile->compile;test->test")
  .dependsOn(server % "compile->compile;test->test")
