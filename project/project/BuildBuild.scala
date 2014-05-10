import sbt._

object BuildBuild extends Build {
  override lazy val settings = super.settings ++ sbtPlugins ++ Seq(
    Keys.scalaVersion := "2.10.4"
  )

  def sbtPlugins = Seq(
    "com.github.malliina" %% "sbt-utils" % "0.0.3",
    "com.timushev.sbt" % "sbt-updates" % "0.1.6"
  ) map addSbtPlugin
}