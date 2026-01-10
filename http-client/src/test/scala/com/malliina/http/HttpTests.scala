package com.malliina.http

import cats.effect.IO
import com.malliina.values.Username
import io.circe.Json

import java.nio.charset.StandardCharsets
import java.util.Base64
import concurrent.duration.DurationInt

object HttpTests {
  val client =
    HttpClient.resource[IO](defaultHeaders = Map(HttpHeaders.`User-Agent` -> "JavaTest/11"))
}

class HttpTests extends munit.CatsEffectSuite {
  val testClient = ResourceFunFixture(HttpTests.client)
  val url = FullUrl("http", "localhost:9000", "/headers")

  testClient.test("Java HTTP client throws on restricted headers") { client =>
    val iae = intercept[IllegalArgumentException] {
      client.postString(url, "content", HttpHeaders.text.plain, Map("content-length" -> "12"))
    }
    assertEquals(iae.getMessage, """restricted header name: "content-length"""")
  }

  testClient.test("Java HTTP".ignore) { client =>
    client.getAs[Json](url).map { res =>
      println(res)
    }
  }

  testClient.test("Java HTTP socket".ignore) { client =>
    val url = FullUrl.wss("logs.malliina.com", "/ws/sources")
    val headers =
      Map(
        HttpHeaders.Authorization -> TestAuth.authorizationValue(Username.unsafe("test"), "test123")
      )
    client.socket(url, headers).use { socket =>
      val events = socket.events
        .take(3)
        .concurrently(fs2.Stream.eval(socket.sendMessage("aaa")).delayBy(2.seconds))
        .evalTap(e => IO(println(e)))
        .compile
        .toVector
      events
    }
  }
}

object TestAuth {
  def authorizationValue(username: Username, password: String): String =
    "Basic " + Base64.getEncoder.encodeToString(
      s"$username:$password".getBytes(StandardCharsets.UTF_8)
    )
}
