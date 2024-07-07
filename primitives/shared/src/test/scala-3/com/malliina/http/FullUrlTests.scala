package com.malliina.http

import com.malliina.http.UrlSyntax.{https, wss}

class FullUrlTests extends munit.FunSuite {
  test("HTTPS literal") {
    val url = https"www.google.com"
    assertEquals(url.url, "https://www.google.com")
  }

  test("HTTPS literal") {
    val url = wss"www.google.com"
    assertEquals(url.url, "wss://www.google.com")
  }
}
