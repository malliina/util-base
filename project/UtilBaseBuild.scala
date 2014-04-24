import com.mle.sbtutils.SbtUtils._
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = Project("util-base",file(".")).settings(utilSettings: _*)

  lazy val utilSettings = publishSettings ++ Seq(
    scalaVersion := "2.11.0",
    version := "0.1.1",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    crossScalaVersions := Seq("2.11.0", "2.10.4")
  )
}