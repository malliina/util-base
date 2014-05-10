import com.mle.sbtutils.SbtUtils._
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = testableProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = publishSettings ++ Seq(
    scalaVersion := "2.11.0",
    version := "0.1.3",
    gitUserName := "malliina",
    developerName := "Michael Skogberg",
    crossScalaVersions := Seq("2.11.0", "2.10.4")
  )
}