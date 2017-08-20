package com.malliina.http

import java.util.regex.Pattern

import com.malliina.values.{ErrorMessage, ValidatingCompanion}

case class FullUrl(proto: String, hostAndPort: String, uri: String) {
  val host = hostAndPort.takeWhile(_ != ':')
  val protoAndHost = s"$proto://$hostAndPort"
  val url = s"$protoAndHost$uri"

  def /(more: String) = append(more.dropWhile(_ == '/'))

  def +(more: String) = append(more)

  def append(more: String): FullUrl = FullUrl(proto, hostAndPort, s"$uri$more")

  def withUri(uri: String) = FullUrl(proto, hostAndPort, uri)

  override def toString: String = url
}

object FullUrl extends ValidatingCompanion[String, FullUrl] {
  val urlPattern = Pattern compile """(.+)://([^/]+)(/?.*)"""

  override def write(t: FullUrl) = t.url

  override def build(input: String): Either[ErrorMessage, FullUrl] = {
    val m = urlPattern.matcher(input)
    if (m.find() && m.groupCount() == 3) {
      Right(FullUrl(m group 1, m group 2, m group 3))
    } else {
      Left(defaultError(input))
    }
  }
}
