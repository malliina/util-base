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
    publishArtifact in Test := false,
    pomExtra := myGitPom(name.value)
  ) ++ credentialsSettings(Path.userHome / ".ivy2" / "sonatype.txt")

  def credentialsSettings(file: File): Seq[Def.Setting[_]] =
    Credentials.loadCredentials(file)
      .fold(err => None, creds => Some(creds))
      .map(creds => credentials += creds)
      .toSeq

  def myGitPom(projectName: String) =
    SbtHelpers.gitPom(projectName, "malliina", "Michael Skogberg", "http://mskogberg.info")
}