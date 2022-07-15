package com.malliina.http.io

import cats.effect.IO
import cats.effect.kernel.{Ref, Temporal}
import com.malliina.http.FullUrl
import com.malliina.http.HttpClient.requestFor
import com.malliina.http.io.SocketEvent.{BytesMessage, Open}
import com.malliina.values.Username
import munit.FunSuite
import okhttp3.{OkHttpClient, Response, WebSocket, WebSocketListener}
import okhttp3.WebSocket.Factory
import okio.ByteString
import fs2.concurrent.Topic
import fs2.Stream
import cats.effect.SyncIO
import concurrent.duration.DurationInt
import java.nio.charset.StandardCharsets
import java.util.Base64

class HttpClientIOTests extends munit.CatsEffectSuite {
  val Authorization = "Authorization"
  val httpFixture: SyncIO[FunFixture[HttpClientIO]] = ResourceFixture(HttpClientIO.resource)

  httpFixture.test("can make io request".ignore) { client =>
    val res = client.get(FullUrl("http", "www.google.com", ""))
    res.map { r => assert(r.isSuccess) }
  }

  httpFixture.test("websocket".ignore) { client =>
    WebSocketIO(
      //FullUrl.wss("logs.malliina.com", "/ws/sources"),
      FullUrl.ws("localhost:9000", "/ws/sources"),
      Map(Authorization -> authorizationValue(Username("test"), "test123")),
      client.client
    ).use { socket =>
      val events: IO[Vector[SocketEvent]] =
        socket.events.take(20).evalTap(e => IO(println(e))).compile.toVector
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
