import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}

val munit = "org.scalameta" %% "munit" % "0.7.23" % Test

inThisBuild(
  Seq(
    releaseCrossBuild := true,
    scalaVersion := "2.13.5",
    crossScalaVersions := scalaVersion.value :: "2.12.13" :: Nil,
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions += "-target:jvm-1.8",
    testFrameworks += new TestFramework("munit.Framework")
  )
)

val moduleSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-json" % "2.9.2",
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

val utilBase = Project("util-base", file("util-base"))
  .dependsOn(primitivesJvm)
  .enablePlugins(MavenCentralPlugin)
  .settings(moduleSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.neovisionaries" % "nv-websocket-client" % "2.14"
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClient = Project("okclient", file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "4.9.1",
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClientIo = Project("okclient-io", file("okclient-io"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(okClient)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.5.0",
      "org.typelevel" %% "cats-effect" % "2.4.1",
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val utilBaseRoot = project
  .in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs, okClient, okClientIo)
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
