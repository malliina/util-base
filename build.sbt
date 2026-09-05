import sbtcrossproject.CrossPlugin.autoImport.{CrossType, crossProject}

import scala.sys.process.Process

val munit = "org.scalameta" %% "munit" % versions.munit % Test

val updateDocs = taskKey[Unit]("Updates README.md")

inThisBuild(
  Seq(
    releaseCrossBuild := true,
    scalaVersion := versions.scala3,
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg"
  )
)

val moduleSettings = Seq(
  libraryDependencies ++= Seq("generic", "parser").map { m =>
    "io.circe" %% s"circe-$m" % versions.circe
  } ++ Seq(
    "org.typelevel" %% "case-insensitive" % versions.ci,
    munit
  )
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
    libraryDependencies ++= Seq("ember-server", "circe", "dsl").map { m =>
      "org.http4s" %% s"http4s-$m" % versions.http4s % Test
    } ++ Seq(
      "org.slf4j" % "slf4j-api" % versions.slf4j,
      "co.fs2" %% "fs2-core" % versions.fs2,
      "org.typelevel" %% "cats-effect" % versions.catsEffect,
      munit,
      "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test,
      "ch.qos.logback" % s"logback-classic" % versions.logback
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClient = project
  .in(file("okclient"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm, httpClient, httpClient % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp-jvm" % versions.okhttp,
      munit
    ),
    releaseProcess := tagReleaseProcess.value
  )

val okClientIo = Project("okclient-io", file("okclient-io"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(okClient, okClient % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % versions.fs2,
      "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test
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

val fs2 = project
  .in(file("fs2"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm)
  .settings(
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "co.fs2" %% "fs2-core" % versions.fs2,
      "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test
    ),
    moduleName := "logback-fs2",
    releaseProcess := tagReleaseProcess.value
  )

val logstreams = project
  .in(file("logstreams-client"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(fs2, okClientIo)
  .settings(
    moduleName := "logstreams-client",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "munit-cats-effect" % versions.munitCats % Test
    ),
    releaseProcess := tagReleaseProcess.value
  )

val webAuth = Project("web-auth", file("web-auth"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitivesJvm, okClientIo)
  .settings(
    Test / publishArtifact := true,
    libraryDependencies ++= Seq(
      "com.nimbusds" % "nimbus-jose-jwt" % versions.nimbusJwt,
      "commons-codec" % "commons-codec" % versions.commonsCodec,
      "org.scalameta" %% "munit" % versions.munit % Test
    ),
    releaseProcess := tagReleaseProcess.value
  )

val html = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("util-html"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(primitives)
  .settings(
    name := "util-html",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "case-insensitive" % versions.ci,
      "com.lihaoyi" %% "scalatags" % versions.scalatags,
      "org.scalameta" %% "munit" % versions.munit % Test
    ),
    releaseProcess := tagReleaseProcess.value
  )

val htmlJvm = html.jvm
val htmlJs = html.js

val database = project
  .in(file("database"))
  .enablePlugins(MavenCentralPlugin)
  .dependsOn(config, okClientIo)
  .settings(
    libraryDependencies ++=
      Seq("core", "hikari").map { m =>
        "org.tpolecat" %% s"doobie-$m" % versions.doobie
      } ++ Seq(
        "org.flywaydb" % "flyway-mysql" % versions.flywayMysql,
        "org.scalameta" %% "munit" % versions.munit % Test
      ),
    releaseProcess := tagReleaseProcess.value
  )

val http4s = project
  .in(file("http4s"))
  .dependsOn(htmlJvm)
  .enablePlugins(MavenCentralPlugin)
  .settings(
    name := "util-http4s",
    libraryDependencies ++=
      Seq("ember-server", "circe", "dsl").map { m =>
        "org.http4s" %% s"http4s-$m" % versions.http4s
      } ++ Seq(
        "org.scalameta" %% "munit" % versions.munit % Test
      ),
    releaseProcess := tagReleaseProcess.value
  )

val docs = project
  .in(file("mdoc"))
  .enablePlugins(MdocPlugin)
  .settings(
    publish / skip := true,
    mdocVariables := Map("VERSION" -> version.value),
    mdocOut := (ThisBuild / baseDirectory).value,
    updateDocs := Def.uncached {
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
  .aggregate(
    utilBase,
    primitivesJvm,
    primitivesJs,
    httpClient,
    okClient,
    okClientIo,
    config,
    fs2,
    logstreams,
    webAuth,
    htmlJvm,
    htmlJs,
    database,
    http4s
  )
  .settings(
    crossScalaVersions := Nil,
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
    publish / skip := true,
    publishArtifact := false,
    packagedArtifacts := Def.uncached(Map.empty),
    publish := {},
    publishLocal := {},
    releaseProcess := (okClient / tagReleaseProcess).value,
    beforeCommitRelease := (docs / updateDocs).value
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
