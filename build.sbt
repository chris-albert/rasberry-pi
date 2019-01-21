import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.lbert",
      scalaVersion := "2.12.7",
      version      := "0.0.3"
    )),
    name := "rasberry-pi",
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      fs2,
      fs2IO,
      atto,
      scalaTest % Test,
      scalaCheck % Test
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
    dockerEntrypoint := Seq("/opt/docker/bin/main"),
    dockerBaseImage := "hypriot/rpi-java",
    mainClass in (Compile, packageBin) := Some("io.lbert.rasberry.Main"),
    resolvers += Resolver.sonatypeRepo("releases"),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),

  ).enablePlugins(JavaAppPackaging, DockerPlugin)
