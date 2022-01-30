import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}

val munit = "org.scalameta" %% "munit" % "0.7.29" % Test

inThisBuild(
  Seq(
    releaseCrossBuild := true,
    scalaVersion := "3.1.1",
    crossScalaVersions := scalaVersion.value :: "2.13.8" :: "2.12.15" :: Nil,
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg",
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val circeModules = Seq("circe-generic", "circe-parser")

val moduleSettings = Seq(
  libraryDependencies ++= circeModules.map(m => "io.circe" %% m % "0.14.1") ++ Seq(
    munit
  )
)
val primitives = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("primitives"))
  .enablePlugins(MavenCentralPlugin)
  .settings(moduleSettings)
val primitivesJvm = primitives.jvm
val primitivesJs = primitives.js

val utilBase = project
  .in(file("util-base"))
  .dependsOn(primitivesJvm)
  .enablePlugins(MavenCentralPlugin)
  .settings(moduleSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.neovisionaries" % "nv-websocket-client" % "2.14"
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClient = project
  .in(file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "4.9.3",
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val logbackModules = Seq("classic", "core")

val okClientIo = Project("okclient-io", file("okclient-io"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(okClient)
  .settings(
    libraryDependencies ++=
      logbackModules.map(m => "ch.qos.logback" % s"logback-$m" % "1.2.10") ++ Seq(
        "co.fs2" %% "fs2-core" % "3.2.3",
        munit
      ),
    releaseProcess := tagReleaseProcess.value
  )

val config = project
  .in(file("config"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq("com.typesafe" % "config" % "1.4.1")
  )

val utilBaseRoot = project
  .in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs, okClient, okClientIo, config)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    publish / skip := true,
    publishArtifact := false,
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {},
    releaseProcess := (okClient / tagReleaseProcess).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
