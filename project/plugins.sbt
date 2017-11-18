scalaVersion := "2.12.4"
resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

addSbtPlugin("com.malliina" %% "sbt-utils" % "0.7.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")
