package com.malliina.http.io

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift, IO, Timer}
import com.malliina.http.FullUrl
import com.malliina.http.HttpClient.requestFor
import com.malliina.http.io.SocketEvent.{Failure, Idle}
import com.malliina.http.io.WebSocketIO.log
import com.malliina.util.AppLogger
import com.malliina.values.Username
import fs2.Stream
import fs2.concurrent.{SignallingRef, Topic}
import okhttp3._
import okio.ByteString
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object WebSocketIO {
  private val log = AppLogger(getClass)

  def apply(url: FullUrl, headers: Map[String, String], client: OkHttpClient)(
    implicit cs: ContextShift[IO],
    t: Timer[IO]
  ): IO[WebSocketIO] = for {
    topic <- Topic[IO, SocketEvent](SocketEvent.Idle)
    interrupter <- SignallingRef[IO, Boolean](false)
  } yield new WebSocketIO(url, headers, client, topic, interrupter)
}

class WebSocketIO(
  val url: FullUrl,
  headers: Map[String, String],
  client: OkHttpClient,
  topic: Topic[IO, SocketEvent],
  interrupter: SignallingRef[IO, Boolean]
)(
  implicit val cs: ContextShift[IO],
  c: Concurrent[IO],
  t: Timer[IO]
) {
  private val backoffTime: FiniteDuration = 10.seconds
  private val active = new AtomicReference[Option[WebSocket]](None)
  private def connectSocket(): IO[WebSocket] = connect.flatMap { socket =>
    IO(active.set(Option(socket))).map(_ => socket)
  }
  private val backoff =
    Stream.eval(IO(log.info(s"Reconnecting to '$url' in $backoffTime..."))).flatMap { _ =>
      Stream.sleep(backoffTime).map(_ => Idle)
    }
  private val allEvents: fs2.Stream[IO, SocketEvent] = topic.subscribe(10)
  import SocketEvent._
  val untilFailure = allEvents.drop(1).takeWhile {
    case Failure(_, _, _) => false
    case _                => true
  }
  val events = Stream
    .eval(connectSocket())
    .flatMap(_ => untilFailure ++ backoff)
    .repeat
    .interruptWhen(interrupter)
  val messages: Stream[IO, String] = events.collect {
    case SocketEvent.TextMessage(_, message) => message
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

  def send(message: String): Boolean = active.get().map(_.send(message)).getOrElse(false)

  def close(): Unit = {
    log.info(s"Closing socket to '$url'...")
    interrupter.set(true).unsafeRunSync()
    active.get().foreach(_.cancel())
  }
}
