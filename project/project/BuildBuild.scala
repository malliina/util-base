import sbt._
import sbt.Keys._

object BuildBuild extends Build {
  override lazy val settings = super.settings ++ sbtPlugins ++ Seq(
    Keys.scalaVersion := "2.10.4",
    resolvers += Resolver.url(
      "bintray-sbt-plugin-releases",
      url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

  )

  def sbtPlugins = Seq(
    "com.github.malliina" %% "sbt-utils" % "0.1.0",
    "me.lessis" % "bintray-sbt" % "0.2.1"
  ) map addSbtPlugin
}
