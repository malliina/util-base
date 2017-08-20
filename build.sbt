import com.malliina.sbtutils.SbtProjects
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}

lazy val p = SbtProjects.mavenPublishProject("util-base")

scalaVersion := "2.12.3"
crossScalaVersions := Seq("2.10.6", "2.11.11", scalaVersion.value)
releaseCrossBuild := true
gitUserName := "malliina"
organization := "com.malliina"
developerName := "Michael Skogberg"
libraryDependencies ++= Seq(
  "io.reactivex" %% "rxscala" % "0.26.5",
  "com.neovisionaries" % "nv-websocket-client" % "2.3"
)
libraryDependencies += {
  "com.typesafe.play" %% "play-json" % "2.6.3"
}
javacOptions ++= Seq("-source", "1.6", "-target", "1.6")
scalacOptions += "-target:jvm-1.6"
