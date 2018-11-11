import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import com.malliina.sbtutils.{SbtProjects, SbtUtils}

import sbtcrossproject.CrossPlugin.autoImport.{CrossType => PortableType, crossProject => portableProject}

lazy val utilBaseRoot = project.in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs, okClient)
  .settings(rootSettings)
lazy val utilBase = SbtProjects.testableProject("util-base", file("util-base"))
  .settings(utilBaseSettings)
  .dependsOn(primitivesJvm)
lazy val primitives = portableProject(JSPlatform, JVMPlatform)
  .crossType(PortableType.Full)
  .in(file("primitives"))
  .settings(moduleSettings)
lazy val primitivesJvm = primitives.jvm
lazy val primitivesJs = primitives.js
lazy val okClient = SbtProjects.testableProject("okclient", file("okclient"))
  .settings(okClientSettings)
  .dependsOn(primitivesJvm)

lazy val basicSettings = Seq(
  releaseCrossBuild := true,
  scalaVersion := "2.12.7",
  crossScalaVersions := scalaVersion.value :: "2.11.12" :: Nil,
  gitUserName := "malliina",
  organization := "com.malliina",
  developerName := "Michael Skogberg"
)

lazy val rootSettings = basicSettings ++ Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val moduleSettings = SbtUtils.mavenSettings ++ basicSettings ++ Seq(
  libraryDependencies += {
    // Uses play-json 2.3.x on 2.11.x since 2.6.x contains JDK8 dependencies which we don't want on Android
    val playJsonVersion = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, minor)) if minor > 11 => "2.6.10"
      case _ => "2.3.10"
    }
    "com.typesafe.play" %% "play-json" % playJsonVersion
  },
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  scalacOptions += "-target:jvm-1.6"
)

lazy val utilBaseSettings = moduleSettings ++ Seq(
  libraryDependencies ++= Seq(
    "io.reactivex" %% "rxscala" % "0.26.5",
    "com.neovisionaries" % "nv-websocket-client" % "2.6"
  )
)

lazy val okClientSettings = SbtUtils.mavenSettings ++ basicSettings ++ Seq(
  libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "3.11.0"
)
