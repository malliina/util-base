import com.mle.sbtutils.{SbtProjects, SbtUtils}
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = SbtProjects.mavenPublishProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = Seq(
    scalaVersion := "2.11.2",
    version := "0.3.0",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    crossScalaVersions := Seq("2.11.2", "2.10.4"),
    libraryDependencies ++= Seq(
      "io.reactivex" % "rxscala_2.11" % "0.22.0",
      "com.typesafe.play" %% "play-json" % "2.3.5",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0"
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6",
    resolvers ++= Seq(
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/")
  )
}