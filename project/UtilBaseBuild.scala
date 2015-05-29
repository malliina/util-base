import bintray.Plugin.bintraySettings
import com.mle.sbtutils.SbtProjects
import sbt.Keys._
import sbt._
import com.mle.sbtutils.SbtUtils.{gitUserName, developerName}

object UtilBaseBuild extends Build {
  lazy val p = SbtProjects.testableProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = bintraySettings ++ Seq(
    scalaVersion := "2.11.6",
    version := "0.7.0",
    gitUserName := "malliina",
    organization := s"com.github.${gitUserName.value}",
    developerName := "Michael Skogberg",
    crossScalaVersions := Seq(scalaVersion.value, "2.10.4"),
    libraryDependencies ++= Seq(
      "io.reactivex" %% "rxscala" % "0.24.1",
      "com.typesafe.play" %% "play-json" % "2.4.0",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0"
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )
}
