import xerial.sbt.Sonatype
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = Project("util-base", file(".")).settings(utilSettings: _*)

  lazy val utilSettings = publishSettings ++ Seq(
    version := "0.0.4",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "1.9.2" % "test"
    )
  )

  def publishSettings = Sonatype.sonatypeSettings ++ Seq(
    organization := "com.github.malliina",
    // The Credentials object must be a DirectCredentials. We obtain one using loadCredentials(File).
    credentials += loadDirectCredentials(Path.userHome / ".ivy2" / "sonatype.txt"),
    publishArtifact in Test := false,
    pomExtra := myGitPom(name.value)
  )

  def loadDirectCredentials(file: File) =
    Credentials.loadCredentials(file).fold(
      errorMsg => throw new Exception(errorMsg),
      cred => cred)

  def myGitPom(projectName: String) =
    SbtHelpers.gitPom(projectName, "malliina", "Michael Skogberg", "http://mskogberg.info")
}