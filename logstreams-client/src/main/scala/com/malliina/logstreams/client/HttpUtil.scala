package com.malliina.logstreams.client

import com.malliina.http.HttpHeaders

import java.nio.charset.StandardCharsets
import java.util.Base64

object HttpUtil:
  val Authorization = HttpHeaders.Authorization
  val Basic = HttpHeaders.Basic
  val UserAgent = HttpHeaders.`User-Agent`

  def basicAuth(username: String, password: String): KeyValue =
    KeyValue(Authorization, authorizationValue(username, password))

  def authorizationValue(username: String, password: String) =
    val bytes = s"$username:$password".getBytes(StandardCharsets.UTF_8)
    val bytesStringified = Base64.getEncoder.encodeToString(bytes)
    s"$Basic $bytesStringified"
