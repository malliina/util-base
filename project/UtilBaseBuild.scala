import com.mle.sbtutils.{SbtProjects, SbtUtils}
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = SbtProjects.mavenPublishProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = Seq(
    scalaVersion := "2.11.5",
    version := "0.4.0",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    crossScalaVersions := Seq(scalaVersion.value, "2.10.4"),
    libraryDependencies ++= Seq(
      "io.reactivex" %% "rxscala" % "0.23.1",
      "com.typesafe.play" %% "play-json" % "2.3.5",
      "org.java-websocket" % "Java-WebSocket" % "1.3.0"
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6"),
    scalacOptions += "-target:jvm-1.6",
    resolvers ++= Seq(
      "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/")
  )
}