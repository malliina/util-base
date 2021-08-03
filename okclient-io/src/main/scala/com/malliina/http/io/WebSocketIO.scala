package com.malliina.http.io

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, IO, Timer}
import com.malliina.http.FullUrl
import com.malliina.http.HttpClient.requestFor
import com.malliina.http.io.SocketEvent.Failure
import com.malliina.http.io.WebSocketIO.log
import com.malliina.util.AppLogger
import com.malliina.values.Username
import fs2.concurrent.{SignallingRef, Topic}
import okhttp3.*
import okio.ByteString
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicReference

object WebSocketIO {
  private val log = AppLogger(getClass)

  def apply(url: FullUrl, headers: Map[String, String], client: OkHttpClient)(
    implicit cs: ContextShift[IO],
    t: Timer[IO]
  ): IO[ReconnectingSocket] = for {
    topic <- Topic[IO, SocketEvent](SocketEvent.Idle)
    specs = new WebSocketIO(url, headers, client, topic)
    socket <- ReconnectingSocket(specs)
  } yield socket
}

class WebSocketIO(
  val url: FullUrl,
  headers: Map[String, String],
  client: OkHttpClient,
  topic: Topic[IO, SocketEvent]
)(
  implicit val cs: ContextShift[IO]
) {
  private val allEvents: fs2.Stream[IO, SocketEvent] = topic.subscribe(10)
  import SocketEvent.*
  val events = allEvents.drop(1).takeWhile {
    case Failure(_, _, _) => false
    case _                => true
  }
  private val listener: WebSocketListener = new WebSocketListener {
    override def onClosed(webSocket: WebSocket, code: Int, reason: String) = {
      log.info(s"Closed  socket to '$url'.")
      topic.push(Closed(webSocket, code, reason))
    }
    override def onClosing(webSocket: WebSocket, code: Int, reason: String) = {
      log.info(s"Closing socket to '$url'.")
      topic.push(Closing(webSocket, code, reason))
    }
    override def onFailure(webSocket: WebSocket, t: Throwable, response: Response) = {
      log.warn(s"Socket to '$url' failed.", t)
      topic.push(Failure(webSocket, Option(t), Option(response)))
    }
    override def onMessage(webSocket: WebSocket, text: String) = {
      log.debug(s"Received text '$text'.")
      topic.push(TextMessage(webSocket, text))
    }
    override def onMessage(webSocket: WebSocket, bytes: ByteString) = {
      log.debug(s"Received bytes $bytes")
      topic.push(BytesMessage(webSocket, bytes))
    }
    override def onOpen(webSocket: WebSocket, response: Response) = {
      log.info(s"Opened socket to '$url'.")
      topic.push(Open(webSocket, response))
    }
  }
  val request = requestFor(url, headers).build()
  val connect = IO(client.newWebSocket(request, listener))
}
