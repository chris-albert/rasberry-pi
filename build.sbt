import Dependencies._
import ReleaseTransformations._
import com.typesafe.sbt.packager.docker._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.lbert",
      scalaVersion := "2.12.7",
      version      := "0.0.3"
    )),
    name := "rasberry-pi",
    libraryDependencies ++= Seq(
      zio,
      zioStreams,
      zioCats,
      http4sBlazeServer,
      http4sDsl,
      http4sCirce,
      scalaTest % Test,
      scalaCheck % Test,
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
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
  ).enablePlugins(JavaAppPackaging, DockerPlugin)
