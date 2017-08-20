import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import com.malliina.sbtutils.{SbtProjects, SbtUtils}

lazy val root = project.in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs)
  .settings(rootSettings)
lazy val utilBase = SbtProjects.testableProject("util-base", file("util-base"))
  .settings(utilBaseSettings)
  .dependsOn(primitivesJvm)
lazy val primitives = crossProject.in(file("primitives"))
  .settings(moduleSettings)
lazy val primitivesJvm = primitives.jvm
lazy val primitivesJs = primitives.js

lazy val basicSettings = Seq(
  scalaVersion := "2.12.3",
  crossScalaVersions := Seq("2.10.6", "2.11.11", scalaVersion.value),
  gitUserName := "malliina",
  organization := "com.malliina",
  developerName := "Michael Skogberg"
)

lazy val rootSettings = basicSettings ++ Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val moduleSettings = basicSettings ++ SbtUtils.mavenSettings ++ Seq(
  releaseCrossBuild := true,
  libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.6.3",
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  scalacOptions += "-target:jvm-1.6"
)
lazy val utilBaseSettings = moduleSettings ++ Seq(
  libraryDependencies ++= Seq(
    "io.reactivex" %% "rxscala" % "0.26.5",
    "com.neovisionaries" % "nv-websocket-client" % "2.3"
  )
)
