package com.malliina.http

object HttpHeaders extends HttpHeaders

trait HttpHeaders {
  val Authorization = "Authorization"
  val `Content-Type` = "Content-Type"
  object application {
    val octetStream = "application/octet-stream"
    val form = "application/x-www-form-urlencoded"
    val json = "application/json"
  }
  object text {
    val plain = "text/plain"
  }
}
