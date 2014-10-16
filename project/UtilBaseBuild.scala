import com.mle.sbtutils.{SbtProjects, SbtUtils}
import sbt.Keys._
import sbt._

object UtilBaseBuild extends Build {
  lazy val p = SbtProjects.testableProject("util-base").settings(utilSettings: _*)

  lazy val utilSettings = SbtUtils.publishSettings ++ Seq(
    scalaVersion := "2.11.2",
    version := "0.2.1",
    SbtUtils.gitUserName := "malliina",
    SbtUtils.developerName := "Michael Skogberg",
    crossScalaVersions := Seq("2.11.2", "2.10.4"),
    libraryDependencies += "io.reactivex" % "rxscala_2.11" % "0.22.0"
  )
}