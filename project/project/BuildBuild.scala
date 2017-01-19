import sbt._
import sbt.Keys._

object BuildBuild {
  lazy val settings = sbtPlugins ++ Seq(
    scalaVersion := "2.10.6",
    resolvers += Resolver.url(
      "bintray-sbt-plugin-releases",
      url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

  )

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-utils" % "0.5.0"
  ) map addSbtPlugin
}
