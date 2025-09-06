package com.malliina.http.io

import cats.effect.IO
import com.malliina.http.FullUrl
import io.circe.Json

class HttpTests extends munit.CatsEffectSuite {
  val http = ResourceFixture(HttpClientIO.resource[IO])
  val url = FullUrl("http", "localhost:9000", "/headers")

  http.test("okhttp".ignore) { client =>
    println(url)
    client.getAs[Json](url).map { res =>
      println(res)
    }
  }
}
