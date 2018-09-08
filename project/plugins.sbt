scalaVersion := "2.12.6"
resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "http://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "com.malliina" %% "sbt-utils" % "0.9.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.5.0",
  "org.scala-js" % "sbt-scalajs" % "0.6.25"
) map addSbtPlugin
