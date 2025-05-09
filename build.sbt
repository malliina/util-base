import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}
import scala.sys.process.Process

val munit = "org.scalameta" %% "munit" % "1.1.0" % Test

val updateDocs = taskKey[Unit]("Updates README.md")

inThisBuild(
  Seq(
    releaseCrossBuild := true,
    scalaVersion := "3.3.1",
    crossScalaVersions := scalaVersion.value :: "2.13.16" :: "2.12.20" :: Nil,
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg"
  )
)

val moduleSettings = Seq(
  libraryDependencies ++= Seq("generic", "parser")
    .map(m => "io.circe" %% s"circe-$m" % "0.14.12") ++ Seq(munit)
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

val okClient = project
  .in(file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "4.12.0",
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClientIo = Project("okclient-io", file("okclient-io"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(okClient)
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "2.0.16",
      "co.fs2" %% "fs2-core" % "3.11.0",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
    ),
    releaseProcess := tagReleaseProcess.value
  )

val config = project
  .in(file("config"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq("com.typesafe" % "config" % "1.4.3") ++ Seq(munit)
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
  .aggregate(utilBase, primitivesJvm, primitivesJs, okClient, okClientIo, config)
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
