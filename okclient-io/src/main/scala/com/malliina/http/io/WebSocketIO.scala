package com.malliina.http.io

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, IO}
import com.malliina.http.FullUrl
import com.malliina.http.HttpClient.requestFor
import com.malliina.values.Username
import fs2.concurrent.{SignallingRef, Topic}
import okhttp3.{OkHttpClient, Request, Response, WebSocket, WebSocketListener}
import okio.ByteString

import java.util.concurrent.atomic.AtomicReference

object WebSocketIO {
  def apply(url: FullUrl, headers: Map[String, String], client: OkHttpClient)(
    implicit cs: ContextShift[IO]
  ): IO[WebSocketIO] = {
    Topic[IO, SocketEvent](SocketEvent.Idle).map { t =>
      new WebSocketIO(url, headers, client, t)(cs)
    }
  }
}

class WebSocketIO(
  url: FullUrl,
  headers: Map[String, String],
  client: OkHttpClient,
  topic: Topic[IO, SocketEvent]
)(
  implicit val cs: ContextShift[IO]
) {
  val events: fs2.Stream[IO, SocketEvent] = topic.subscribe(10)
  private val interrupts = events.collect {
    case SocketEvent.Failure(_, _, _) => true
  }
  val messages: fs2.Stream[IO, String] = events
    .collect {
      case SocketEvent.TextMessage(_, message) => message
    }
    .interruptWhen(interrupts)
  private val listener: WebSocketListener = new WebSocketListener {
    override def onClosed(webSocket: WebSocket, code: Int, reason: String) = {
      log(s"closed $reason $webSocket")
      topic.push(SocketEvent.Closed(webSocket, code, reason))
    }
    override def onClosing(webSocket: WebSocket, code: Int, reason: String) = {
      log(s"closing $reason $webSocket")
      topic.push(SocketEvent.Closing(webSocket, code, reason))
    }
    override def onFailure(webSocket: WebSocket, t: Throwable, response: Response) = {
      log(s"failure ${Option(response)}")
      topic.push(
        SocketEvent.Failure(webSocket, Option(t), Option(response))
      )
    }
    override def onMessage(webSocket: WebSocket, text: String) = {
      log(s"text message $text")
      topic.push(SocketEvent.TextMessage(webSocket, text))
    }
    override def onMessage(webSocket: WebSocket, bytes: ByteString) = {
      log(s"bytes message $bytes")
      topic.push(SocketEvent.BytesMessage(webSocket, bytes))
    }
    override def onOpen(webSocket: WebSocket, response: Response) = {
      log(s"open $response $webSocket")
      topic.push(SocketEvent.Open(webSocket, response))
    }
  }
  val request = requestFor(url, headers).build()
  val socket = client.newWebSocket(request, listener)

  def reconnect: IO[WebSocketIO] = WebSocketIO(url, headers, client)

  def send(message: String): Boolean = socket.send(message)

  def log(msg: String) = println(msg)

  def close(): Unit = socket.cancel()
}

object ReconnectingSocket {
  def apply(initial: WebSocketIO): IO[ReconnectingSocket] = {
    implicit val cs = initial.cs
    SignallingRef[IO, Boolean](false).map { i => new ReconnectingSocket(initial, i) }
  }
}

class ReconnectingSocket(initial: WebSocketIO, interrupter: SignallingRef[IO, Boolean])(
  implicit c: Concurrent[IO]
) {
  private val active = new AtomicReference[WebSocketIO](initial)
  val events = initial.events
    .handleErrorWith { t =>
      fs2.Stream.eval(initial.reconnect).flatMap { newSocket =>
        println("handling error...")
        active.set(newSocket)
        newSocket.events
      }
    }
    .interruptWhen(interrupter)

  def send(message: String): Boolean = active.get().send(message)
  def close(): Unit = {
    println("interrupting...")
    interrupter.set(true).unsafeRunSync()
  }
}
