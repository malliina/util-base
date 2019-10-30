scalaVersion := "2.12.10"
resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "https://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "com.malliina" %% "sbt-utils-maven" % "0.14.2",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0",
  "org.scala-js" % "sbt-scalajs" % "0.6.29"
) map addSbtPlugin
