import com.malliina.sbtutils.SbtProjects
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = SbtProjects.testableProject("util-base")
    .settings(utilSettings: _*)
    .enablePlugins(bintray.BintrayPlugin)

  lazy val utilSettings = Seq(
    scalaVersion := "2.11.7",
    version := "1.0.0",
    gitUserName := "malliina",
    organization := s"com.${gitUserName.value}",
    developerName := "Michael Skogberg",
    crossScalaVersions := Seq(scalaVersion.value, "2.10.6"),
    libraryDependencies ++= Seq(
      "io.reactivex" %% "rxscala" % "0.26.0",
      "com.typesafe.play" %% "play-json" % "2.5.1",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0"
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6",
    licenses +=("MIT", url("http://opensource.org/licenses/MIT"))
  )
}
