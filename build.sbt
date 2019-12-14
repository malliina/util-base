import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}

val scalaTestVersion = "3.1.0"
val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test

val basicSettings = Seq(
  releaseCrossBuild := true,
  scalaVersion := "2.13.1",
  crossScalaVersions := scalaVersion.value :: "2.12.10" :: Nil,
  gitUserName := "malliina",
  organization := "com.malliina",
  developerName := "Michael Skogberg",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions += "-target:jvm-1.8"
)
val moduleSettings = basicSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-json" % "2.8.1",
    scalaTest
  )
)
val primitives = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("primitives"))
  .enablePlugins(MavenCentralPlugin)
  .settings(moduleSettings)
  .settings(
    releaseProcess := tagReleaseProcess.value
  )
val primitivesJvm = primitives.jvm
val primitivesJs = primitives.js

val utilBase = Project("util-base", file("util-base"))
  .dependsOn(primitivesJvm)
  .enablePlugins(MavenCentralPlugin)
  .settings(moduleSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.neovisionaries" % "nv-websocket-client" % "2.9"
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClient = Project("okclient", file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(basicSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "4.2.2",
      scalaTest
    ),
    releaseProcess := tagReleaseProcess.value
  )

val utilBaseRoot = project
  .in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs, okClient)
  .settings(basicSettings)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    skip in publish := true,
    publishArtifact := false,
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {},
    releaseProcess := (tagReleaseProcess in okClient).value
  )
