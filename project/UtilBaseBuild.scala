import bintray.Plugin.bintraySettings
import com.mle.sbtutils.SbtProjects
import sbt.Keys._
import sbt._
import com.mle.sbtutils.SbtUtils.{gitUserName, developerName}

object UtilBaseBuild extends Build {
  lazy val p = SbtProjects.testableProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = bintraySettings ++ Seq(
    scalaVersion := "2.11.7",
    version := "0.9.0",
    gitUserName := "malliina",
    organization := s"com.${gitUserName.value}",
    developerName := "Michael Skogberg",
    crossScalaVersions := Seq(scalaVersion.value, "2.10.6"),
    libraryDependencies ++= Seq(
      "io.reactivex" %% "rxscala" % "0.25.1",
      "com.typesafe.play" %% "play-json" % "2.4.2",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0"
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )
}
