package com.malliina.http

import munit.FunSuite

class FullUrlTests extends FunSuite {
  test("url") {
    val url = url"https://www.google.com"
    assertEquals(url.proto, "https")
    assertEquals(url.host, "www.google.com")
    assertEquals(url.uri, "")
  }
}
