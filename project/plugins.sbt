scalaVersion := "2.12.20"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.6.55",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2",
  "org.scala-js" % "sbt-scalajs" % "1.20.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.5",
  "org.scalameta" % "sbt-mdoc" % "2.7.2"
) map addSbtPlugin
