package com.malliina.http.io

import cats.effect.{ContextShift, IO, Timer}
import cats.effect.concurrent.Ref
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

import concurrent.duration.DurationInt
import java.nio.charset.StandardCharsets
import java.util.Base64

class HttpClientIOTests extends FunSuite {
  val Authorization = "Authorization"
  private implicit val cs: ContextShift[IO] = IO.contextShift(munitExecutionContext)
  private implicit val timer: Timer[IO] = IO.timer(munitExecutionContext)

  test("can make io request".ignore) {
    val client = HttpClientIO()
    try {
      val res = client.get(FullUrl("http", "www.google.com", "")).unsafeRunSync()
      assert(res.isSuccess)
    } finally client.close()
  }

  test("websocket".ignore) {
    val client = HttpClientIO()
    val socket = WebSocketIO(
      FullUrl.wss("logs.malliina.com", "/ws/sources"),
      Map(Authorization -> authorizationValue(Username("test"), "test123")),
      client.client
    ).unsafeRunSync()
    val events: IO[Vector[SocketEvent]] = socket.events.take(5).compile.toVector
    events.unsafeRunAsyncAndForget()
    Thread.sleep(3000)
    socket.close()
    Thread.sleep(8000)
    socket.close()
    Thread.sleep(8000)
    socket.close()
    client.close()
  }

  test("interruption".ignore) {
    val start = System.currentTimeMillis()
    val tick =
      Stream(0L) ++ Stream.awakeEvery[IO](1.seconds).map(d => System.currentTimeMillis() - start)
    val interrupter = Stream.sleep(5.seconds).map(_ => true)
    val stream = tick.interruptWhen(interrupter).repeat.take(10)
    val outcome = stream.compile.toVector.unsafeRunSync()
    println(outcome)
  }

  def authorizationValue(username: Username, password: String) =
    "Basic " + Base64.getEncoder.encodeToString(
      s"$username:$password".getBytes(StandardCharsets.UTF_8)
    )
}
