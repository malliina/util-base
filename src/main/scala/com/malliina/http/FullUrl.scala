package com.malliina.http

case class FullUrl(proto: String, hostAndPort: String, uri: String) {
  val host = hostAndPort.takeWhile(_ != ':')
  val protoAndHost = s"$proto://$hostAndPort"
  val url = s"$protoAndHost$uri"

  def /(more: String) = append(more.dropWhile(_ == '/'))

  def +(more: String) = append(more)

  def append(more: String): FullUrl = FullUrl(proto, hostAndPort, s"$uri$more")

  override def toString: String = url
}
