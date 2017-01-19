import com.malliina.sbtutils.SbtProjects
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import sbt.Keys._
import sbt._

object UtilBaseBuild {
  lazy val p = SbtProjects.mavenPublishProject("util-base")
    .settings(utilSettings: _*)

  lazy val utilSettings = Seq(
    scalaVersion := "2.11.8",
    version := "1.0.1",
    gitUserName := "malliina",
    organization := "com.malliina",
    developerName := "Michael Skogberg",
    libraryDependencies ++= Seq(
      "io.reactivex" %% "rxscala" % "0.26.5",
      "com.typesafe.play" %% "play-json" % "2.5.10",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0"
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6"
  )
}
