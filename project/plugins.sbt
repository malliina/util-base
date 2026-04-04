scalaVersion := "2.12.20"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.7.0",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "org.scala-js" % "sbt-scalajs" % "1.21.0",
  "org.scalameta" % "sbt-scalafmt" % "2.5.6",
  "org.scalameta" % "sbt-mdoc" % "2.8.2"
) map addSbtPlugin
