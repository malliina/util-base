package com.malliina.http

import com.malliina.http.UrlSyntax.{https, url, wss}

class FullUrlTests extends munit.FunSuite {
  test("HTTPS literal") {
    val url = https"www.google.com"
    assertEquals(url.url, "https://www.google.com")
  }

  test("WSS literal") {
    val url = wss"www.google.com"
    assertEquals(url.url, "wss://www.google.com")
  }

  test("JDBC literal") {
    val url = url"jdbc:mysql://localhost:3306/database"
    assertEquals(url.url, "jdbc:mysql://localhost:3306/database")
  }
}
