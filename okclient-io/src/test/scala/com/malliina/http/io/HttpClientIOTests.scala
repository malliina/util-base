package com.malliina.http.io

import cats.effect.IO
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

import java.nio.charset.StandardCharsets
import java.util.Base64

class HttpClientIOTests extends FunSuite {
  val Authorization = "Authorization"

  test("can make io request".ignore) {
    val client = HttpClientIO()
    try {
      val res = client.get(FullUrl("http", "www.google.com", "")).unsafeRunSync()
      assert(res.isSuccess)
    } finally client.close()
  }

  test("websocket") {
    implicit val cs = IO.contextShift(munitExecutionContext)
    val client = HttpClientIO()

    val socket = WebSocketIO(
      FullUrl.wss("logs.malliina.com", "/ws/sources"),
      Map(Authorization -> authorizationValue(Username("test"), "test123")),
      client.client
    ).unsafeRunSync()
    log("Canceling...")
    socket.close()
    client.close()
  }

  def authorizationValue(username: Username, password: String) =
    "Basic " + Base64.getEncoder.encodeToString(
      s"$username:$password".getBytes(StandardCharsets.UTF_8)
    )

  def log(msg: String) = println(msg)
}
