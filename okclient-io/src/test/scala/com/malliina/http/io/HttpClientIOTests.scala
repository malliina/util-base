package com.malliina.http.io

import cats.effect.{IO, SyncIO}
import com.malliina.http.{FullUrl, OkHttpHttpClient, SocketEvent}
import com.malliina.values.Username
import fs2.Stream
import okhttp3.WebSocket

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.duration.DurationInt

class HttpClientIOTests extends munit.CatsEffectSuite {
  val Authorization = "Authorization"
  val httpFixture: SyncIO[FunFixture[HttpClientF2[IO]]] = ResourceFixture(HttpClientIO.resource[IO])

  httpFixture.test("can make io request".ignore) { client =>
    val res = client.get(FullUrl("http", "www.google.com", ""))
    res.map(r => assert(r.isSuccess))
  }

  httpFixture.test("websocket".ignore) { client =>
    WebSocketF
      .build[IO](
        FullUrl.wss("logs.malliina.com", "/ws/sources"),
//        FullUrl.ws("localhost:9000", "/ws/sources"),
        Map(Authorization -> authorizationValue(Username("test"), "test1234")),
        client.client
      )
      .use { socket =>
        val events: IO[Vector[SocketEvent]] =
          socket.events.take(3).evalTap(e => IO(println(e))).compile.toVector
        events
      }
  }

  test("interruption".ignore) {
    val start = System.currentTimeMillis()
    val tick =
      Stream(0L) ++ Stream.awakeEvery[IO](1.seconds).map(d => System.currentTimeMillis() - start)
    val interrupter = Stream.sleep[IO](5.seconds).map(_ => true)
    val stream = tick.interruptWhen(interrupter).repeat.take(10)
    val outcome = stream.compile.toVector.unsafeRunSync()
    println(outcome)
  }

  def authorizationValue(username: Username, password: String): String =
    "Basic " + Base64.getEncoder.encodeToString(
      s"$username:$password".getBytes(StandardCharsets.UTF_8)
    )
}
