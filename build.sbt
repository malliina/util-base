import com.malliina.sbtutils.SbtProjects
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}

lazy val p = SbtProjects.mavenPublishProject("util-base")

scalaVersion := "2.12.2"
crossScalaVersions := Seq("2.10.6", "2.11.11", scalaVersion.value)
releaseCrossBuild := true
gitUserName := "malliina"
organization := "com.malliina"
developerName := "Michael Skogberg"
libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.5",
  "org.java-websocket" % "Java-WebSocket" % "1.3.0"
)
libraryDependencies += {
  val playJsonVersion = CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, minor)) if minor >= 11 => "2.6.2"
    case _ => "2.4.10"
  }
  "com.typesafe.play" %% "play-json" % playJsonVersion
}
javacOptions ++= Seq("-source", "1.6", "-target", "1.6")
scalacOptions += "-target:jvm-1.6"
