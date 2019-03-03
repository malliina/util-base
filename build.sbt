import sbtcrossproject.CrossPlugin.autoImport.{
  CrossType => PortableType,
  crossProject => portableProject
}

val basicSettings = Seq(
  releaseCrossBuild := true,
  scalaVersion := "2.12.8",
  crossScalaVersions := scalaVersion.value :: "2.11.12" :: Nil,
  gitUserName := "malliina",
  organization := "com.malliina",
  developerName := "Michael Skogberg"
)
val moduleSettings = basicSettings ++ Seq(
  libraryDependencies ++= {
    // Uses play-json 2.3.x on 2.11.x since 2.6.x contains JDK8 dependencies which we don't want on Android
    val playJsonVersion = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor > 11 => "2.7.1"
      case _                              => "2.3.10"
    }
    Seq(
      "com.typesafe.play" %% "play-json" % playJsonVersion,
      "org.scalatest" %% "scalatest" % "3.0.6" % Test
    )
  },
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  scalacOptions += "-target:jvm-1.6"
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
      "io.reactivex" %% "rxscala" % "0.26.5",
      "com.neovisionaries" % "nv-websocket-client" % "2.6"
    )
  )

val okClient = Project("okclient", file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(basicSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "3.13.1",
      "org.scalatest" %% "scalatest" % "3.0.6" % Test
    )
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
    publishLocal := {}
  )
