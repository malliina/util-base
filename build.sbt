import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import com.malliina.sbtutils.{SbtProjects, SbtUtils}

lazy val utilBaseRoot = project.in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs, okClient)
  .settings(rootSettings)
lazy val utilBase = SbtProjects.testableProject("util-base", file("util-base"))
  .settings(utilBaseSettings)
  .dependsOn(primitivesJvm)
lazy val primitives = crossProject.in(file("primitives"))
  .settings(moduleSettings)
lazy val primitivesJvm = primitives.jvm
lazy val primitivesJs = primitives.js
lazy val okClient = SbtProjects.testableProject("okclient", file("okclient"))
  .settings(okClientSettings)
  .dependsOn(primitivesJvm)

lazy val basicSettings = Seq(
  releaseCrossBuild := true,
  scalaVersion := "2.12.5",
  gitUserName := "malliina",
  organization := "com.malliina",
  developerName := "Michael Skogberg"
)

lazy val rootSettings = basicSettings ++ Seq(
  publishArtifact := false,
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val moduleSettings = SbtUtils.mavenSettings ++ basicSettings ++ Seq(
  libraryDependencies += "com.typesafe.play" %%% "play-json" % "2.6.9",
  javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
  scalacOptions += "-target:jvm-1.6"
)

lazy val utilBaseSettings = moduleSettings ++ Seq(
  libraryDependencies ++= Seq(
    "io.reactivex" %% "rxscala" % "0.26.5",
    "com.neovisionaries" % "nv-websocket-client" % "2.3"
  )
)

lazy val okClientSettings = SbtUtils.mavenSettings ++ basicSettings ++ Seq(
  libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "3.10.0"
)
