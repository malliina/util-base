import sbt._

object BuildBuild extends Build {
  override lazy val settings = super.settings ++ sbtPlugins

  def sbtPlugins = Seq(
    "com.timushev.sbt" % "sbt-updates" % "0.1.2",
    "com.typesafe.sbt" % "sbt-pgp" % "0.8.1",
    "org.xerial.sbt" % "sbt-sonatype" % "0.1.4"
  ) map addSbtPlugin
}