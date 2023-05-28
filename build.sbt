import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}

val munit = "org.scalameta" %% "munit" % "0.7.29" % Test

inThisBuild(
  Seq(
    releaseCrossBuild := true,
    scalaVersion := "3.2.2",
    crossScalaVersions := scalaVersion.value :: "2.13.10" :: "2.12.17" :: Nil,
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg",
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val moduleSettings = Seq(
  libraryDependencies ++= Seq("generic", "parser")
    .map(m => "io.circe" %% s"circe-$m" % "0.14.5") ++ Seq(munit)
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
    releaseProcess := tagReleaseProcess.value
  )

val okClient = project
  .in(file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "4.11.0",
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClientIo = Project("okclient-io", file("okclient-io"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(okClient)
  .settings(
    libraryDependencies ++=
      Seq("classic", "core").map(m => "ch.qos.logback" % s"logback-$m" % "1.4.7") ++ Seq(
        "co.fs2" %% "fs2-core" % "3.7.0",
        "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
      ),
    releaseProcess := tagReleaseProcess.value
  )

val config = project
  .in(file("config"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq("com.typesafe" % "config" % "1.4.2")
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
