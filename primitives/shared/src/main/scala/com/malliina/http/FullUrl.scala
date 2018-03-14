package com.malliina.http

import java.util.regex.Pattern

import com.malliina.values.{ErrorMessage, ValidatingCompanion}

case class FullUrl(proto: String, hostAndPort: String, uri: String) {
  val host = hostAndPort.takeWhile(_ != ':')
  val protoAndHost = s"$proto://$hostAndPort"
  val url = s"$protoAndHost$uri"

  def /(more: String): FullUrl = {
    if (uri.endsWith("/")) append(more.dropWhile(_ == '/'))
    else append(if (more.startsWith("/")) more else s"/$more")
  }

  def +(more: String) = append(more)

  def append(more: String): FullUrl = FullUrl(proto, hostAndPort, s"$uri$more")

  def withUri(uri: String) = FullUrl(proto, hostAndPort, uri)

  def withQuery(qs: (String, String)*) = {
    val asString = qs.map { case (k, v) => s"$k=$v" }.mkString("&")
    val firstChar = if (uri.contains("?")) "&" else "?"
    append(s"$firstChar$asString")
  }

  override def toString: String = url
}

object FullUrl extends ValidatingCompanion[String, FullUrl] {
  val urlPattern = Pattern compile """(.+)://([^/]+)(/?.*)"""

  def https(domain: String, uri: String): FullUrl =
    FullUrl("https", dropHttps(domain), uri)

  def host(domain: String): FullUrl =
    FullUrl("https", dropHttps(domain), "")

  private def dropHttps(domain: String) = {
    val prefix = "https://"
    if (domain.startsWith(prefix)) domain.drop(prefix.length) else domain
  }

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
