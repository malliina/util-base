scalaVersion := "2.12.8"
resolvers ++= Seq(
  ivyResolver("bintray-sbt-plugin-releases", "https://dl.bintray.com/content/sbt/sbt-plugin-releases"),
  ivyResolver("malliina bintray sbt", "https://dl.bintray.com/malliina/sbt-plugins/")
)

def ivyResolver(name: String, urlStr: String) =
  Resolver.url(name, url(urlStr))(Resolver.ivyStylePatterns)

Seq(
  "com.malliina" %% "sbt-utils-maven" % "0.12.1",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0",
  "org.scala-js" % "sbt-scalajs" % "0.6.28",
  "ch.epfl.scala" % "sbt-bloop" % "1.2.5"
) map addSbtPlugin
