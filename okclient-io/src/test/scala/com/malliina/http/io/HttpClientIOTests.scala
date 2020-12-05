package com.malliina.http.io

import com.malliina.http.FullUrl
import munit.FunSuite

class HttpClientIOTests extends FunSuite {
  test("can make io request".ignore) {
    val client = HttpClientIO()
    try {
      val res = client.get(FullUrl("http", "www.google.com", "")).unsafeRunSync()
      assert(res.isSuccess)
    } finally client.close()
  }
}
