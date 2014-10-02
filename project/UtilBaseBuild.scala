import com.mle.sbtutils.SbtUtils._
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = testableProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = publishSettings ++ Seq(
    scalaVersion := "2.11.1",
    version := "0.2.0",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    crossScalaVersions := Seq("2.11.2", "2.10.4")
  )
}