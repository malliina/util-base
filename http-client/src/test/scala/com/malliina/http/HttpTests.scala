package com.malliina.http

import cats.effect.IO
import io.circe.Json

class HttpTests extends munit.CatsEffectSuite {
  val http = ResourceFixture(
    HttpClient.resource[IO](defaultHeaders = Map(HttpHeaders.`User-Agent` -> "JavaTest/11"))
  )
  val url = FullUrl("http", "localhost:9000", "/headers")

  http.test("Java HTTP".ignore) { client =>
    client.getAs[Json](url).map { res =>
      println(res)
    }
  }
}
