import sbt._
import sbt.Keys._

object BuildBuild {
  lazy val settings = sbtPlugins ++ Seq(
    scalaVersion := "2.10.6",
    resolvers ++= Seq(
      ivyResolver("bintray-sbt-plugin-releases", "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
      ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
    )
  )

  def ivyResolver(name: String, urlStr: String) =
    Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

  def sbtPlugins = Seq(
    "com.malliina" %% "sbt-utils" % "0.5.0",
    "com.github.gseitz" % "sbt-release" % "1.0.3"
  ) map addSbtPlugin
}
