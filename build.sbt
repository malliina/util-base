import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import scala.sys.process.Process

val versions = new {
  val scala212 = "2.12.20"
  val scala213 = "2.13.16"
  val scala3 = "3.3.1"

  val catsEffect = "3.5.4"
  val circe = "0.14.14"
  val config = "1.4.4"
  val fs2 = "3.11.0"
  val munit = "1.1.1"
  val munitCats = "1.0.7"
  val okhttp = "4.12.0"
  val slf4j = "2.0.17"
}

val munit = "org.scalameta" %% "munit" % versions.munit % Test

val updateDocs = taskKey[Unit]("Updates README.md")

inThisBuild(
  Seq(
    releaseCrossBuild := true,
    scalaVersion := versions.scala3,
    crossScalaVersions := scalaVersion.value :: versions.scala213 :: versions.scala212 :: Nil,
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg"
  )
)

val moduleSettings = Seq(
  libraryDependencies ++= Seq("generic", "parser")
    .map(m => "io.circe" %% s"circe-$m" % versions.circe) ++ Seq(munit)
)
val primitives = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
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

val httpClient = Project("http-client", file("http-client"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % versions.catsEffect,
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClient = project
  .in(file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm, httpClient)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % versions.okhttp,
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClientIo = Project("okclient-io", file("okclient-io"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(okClient)
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      "co.fs2" %% "fs2-core" % versions.fs2,
      "org.typelevel" %% "munit-cats-effect-3" % versions.munitCats % Test
    ),
    releaseProcess := tagReleaseProcess.value
  )

val config = project
  .in(file("config"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq("com.typesafe" % "config" % versions.config) ++ Seq(munit)
  )

val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin)
  .settings(
    publish / skip := true,
    mdocVariables := Map("VERSION" -> version.value),
    mdocOut := (ThisBuild / baseDirectory).value,
    updateDocs := {
      val log = streams.value.log
      val outFile = mdocOut.value
      IO.relativize((ThisBuild / baseDirectory).value, outFile)
        .getOrElse(sys.error(s"Strange directory: $outFile"))
      val addStatus = Process(s"git add $outFile").run(log).exitValue()
      if (addStatus != 0) {
        sys.error(s"Unexpected status code $addStatus for git commit.")
      }
    },
    updateDocs := updateDocs.dependsOn(mdoc.toTask("")).value
  )

val utilBaseRoot = project
  .in(file("."))
  .aggregate(utilBase, primitivesJvm, primitivesJs, httpClient, okClient, okClientIo, config)
  .settings(
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    publish / skip := true,
    publishArtifact := false,
    packagedArtifacts := Map.empty,
    publish := {},
    publishLocal := {},
    releaseProcess := (okClient / tagReleaseProcess).value,
    beforeCommitRelease := (docs / updateDocs).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
