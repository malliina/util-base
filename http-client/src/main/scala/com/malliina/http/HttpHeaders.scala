package com.malliina.http

object HttpHeaders extends HttpHeaders

trait HttpHeaders {
  val `Accept-Encoding` = "Accept-Encoding"
  val Authorization = "Authorization"
  val `Content-Encoding` = "Content-Encoding"
  val `Content-Type` = "Content-Type"
  val deflate = "deflate"
  val gzip = "gzip"
  val `User-Agent` = "User-Agent"
  object application {
    val octetStream = "application/octet-stream"
    val form = "application/x-www-form-urlencoded"
    val json = "application/json"
  }
  object text {
    val plain = "text/plain"
  }
}
