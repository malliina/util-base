package com.malliina.http

class MimeType(tpe: String):
  def sub(subType: String) = s"$tpe/$subType"

object HttpHeaders extends HttpHeaders

trait HttpHeaders:
  val `Accept-Encoding` = "Accept-Encoding"
  val Authorization = "Authorization"
  val Basic = "Basic"
  val `Content-Encoding` = "Content-Encoding"
  val `Content-Type` = "Content-Type"
  val deflate = "deflate"
  val gzip = "gzip"
  val `User-Agent` = "User-Agent"
  object application extends MimeType("application"):
    val octetStream = sub("octet-stream")
    val form = sub("x-www-form-urlencoded")
    val json = sub("json")
  object text extends MimeType("text"):
    val plain = sub("plain")
