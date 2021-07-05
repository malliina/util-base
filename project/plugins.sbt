scalaVersion := "2.12.13"

Seq(
  "com.malliina" % "sbt-utils-maven" % "1.2.3",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0",
  "org.scala-js" % "sbt-scalajs" % "1.5.1",
  "org.scalameta" % "sbt-scalafmt" % "2.4.2"
) map addSbtPlugin
