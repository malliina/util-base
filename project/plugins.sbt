scalaVersion := "2.12.10"

Seq(
  "com.malliina" % "sbt-utils-maven" % "0.15.7",
  "org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1",
  "org.scala-js" % "sbt-scalajs" % "0.6.31",
  "ch.epfl.scala" % "sbt-bloop" % "1.3.4",
  "org.scalameta" % "sbt-scalafmt" % "2.3.0"
) map addSbtPlugin
