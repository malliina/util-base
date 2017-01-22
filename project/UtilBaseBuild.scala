import com.malliina.sbtutils.SbtProjects
import com.malliina.sbtutils.SbtUtils.{developerName, gitUserName}
import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport.{ReleaseStep, releaseProcess, releasePublishArtifactsAction}
import sbtrelease.ReleaseStateTransformations._

object UtilBaseBuild {
  lazy val p = SbtProjects.mavenPublishProject("util-base")
    .settings(utilSettings: _*)

  lazy val utilSettings = basicSettings ++ releaseSettings

  def basicSettings = Seq(
    scalaVersion := "2.11.8",
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

  def releaseSettings = Seq(
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts, // : ReleaseStep, checks whether `publishTo` is properly set up
      setNextVersion,
      commitNextVersion,
      ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
      pushChanges // : ReleaseStep, also checks that an upstream branch is properly configured
    )
  )
}
