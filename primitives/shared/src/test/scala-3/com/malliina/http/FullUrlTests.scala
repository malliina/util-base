package com.malliina.http

import com.malliina.values.{AccessToken, IdToken}
import com.malliina.http.UrlSyntax.{https, url, wss}

class FullUrlTests extends munit.FunSuite:
  test("HTTPS literal"):
    val url = https"www.google.com"
    assertEquals(url.url, "https://www.google.com")

  test("WSS literal"):
    val url = wss"www.google.com"
    assertEquals(url.url, "wss://www.google.com")

  test("JDBC literal"):
    val url = url"jdbc:mysql://localhost:3306/database"
    assertEquals(url.url, "jdbc:mysql://localhost:3306/database")

  test("Query parameters from typeclass derivation"):
    case class Params(a: String, b: Int) derives KeyValues
    val url: FullUrl = https"www.google.com"
    val full = url.query(KeyValues[Params].kvs(Params("aa", 1)))
    assertEquals(full, https"www.google.com?a=aa&b=1")
    assertEquals(full.uri, "?a=aa&b=1")

  test("Query showable"):
    val url = https"www.google.com"
    val at = AccessToken("at")
    val it = IdToken("it")
    val queryUrl = url.queryShow("a" -> at, "i" -> it)
    assertEquals(queryUrl.url, "https://www.google.com?a=at&i=it")
