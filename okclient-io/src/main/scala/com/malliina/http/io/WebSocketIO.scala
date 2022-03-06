package com.malliina.http.io

import cats.effect.IO
import cats.effect.kernel.{Temporal, Resource}
import cats.effect.std.Dispatcher
import com.malliina.http.FullUrl
import com.malliina.http.io.SocketEvent._
import com.malliina.http.io.WebSocketIO.log
import com.malliina.util.AppLogger
import com.malliina.values.Username
import fs2.Stream
import fs2.concurrent.{SignallingRef, Topic}
import io.circe._
import io.circe.syntax.EncoderOps
import okhttp3._
import okio.ByteString
import org.slf4j.LoggerFactory

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait WebSocketOps {
  def send[T: Encoder](message: T): IO[Boolean] = sendMessage(message.asJson.noSpaces)
  def sendMessage(s: String): IO[Boolean]
}

object WebSocketIO {
  private val log = AppLogger(getClass)

  def apply(url: FullUrl, headers: Map[String, String], client: OkHttpClient)(
    implicit t: Temporal[IO]
  ): Resource[IO, WebSocketIO] =
    for {
      topic <- Resource.eval(Topic[IO, SocketEvent])
      interrupter <- Resource.eval(SignallingRef[IO, Boolean](false))
      d <- Dispatcher[IO]
      socket <- Resource.make(IO(new WebSocketIO(url, headers, client, topic, interrupter, d)))(s =>
        s.close
      )
    } yield socket
}

class WebSocketIO(
  val url: FullUrl,
  headers: Map[String, String],
  client: OkHttpClient,
  topic: Topic[IO, SocketEvent],
  interrupter: SignallingRef[IO, Boolean],
  d: Dispatcher[IO]
)(implicit t: Temporal[IO])
  extends WebSocketOps {
  private val backoffTime: FiniteDuration = 10.seconds
  private val active = new AtomicReference[Option[WebSocket]](None)

  val allEvents: Stream[IO, SocketEvent] = topic.subscribe(10)
  val messages: Stream[IO, String] = allEvents.collect {
    case TextMessage(_, message) => message
  }
  val jsonMessages: Stream[IO, Json] = messages.flatMap { message =>
    parser
      .parse(message)
      .fold(
        err => Stream.raiseError(new Exception(s"Not JSON: '$message'.")),
        ok => Stream.emit(ok)
      )
  }

  private def send(e: SocketEvent): Unit = d.unsafeRunAndForget(topic.publish1(e))

  private val listener: WebSocketListener = new WebSocketListener {
    override def onClosed(webSocket: WebSocket, code: Int, reason: String): Unit = {
      log.info(s"Closed  socket to '$url'.")
      send(Closed(webSocket, code, reason))
    }
    override def onClosing(webSocket: WebSocket, code: Int, reason: String): Unit = {
      log.info(s"Closing socket to '$url'.")
      send(Closing(webSocket, code, reason))
    }
    override def onFailure(webSocket: WebSocket, t: Throwable, response: Response): Unit = {
      log.warn(s"Socket to '$url' failed.", t)
      send(Failure(webSocket, Option(t), Option(response)))
    }
    override def onMessage(webSocket: WebSocket, text: String): Unit = {
      log.debug(s"Received text '$text'.")
      send(TextMessage(webSocket, text))
    }
    override def onMessage(webSocket: WebSocket, bytes: ByteString): Unit = {
      log.debug(s"Received bytes $bytes")
      send(BytesMessage(webSocket, bytes))
    }
    override def onOpen(webSocket: WebSocket, response: Response): Unit = {
      log.info(s"Opened socket to '$url'.")
      send(Open(webSocket, response))
    }
  }
  val request: Request = requestFor(url, headers).build()
  val connectOnce: IO[WebSocket] = IO(client.newWebSocket(request, listener))
  val connectSocket: IO[WebSocket] = connectOnce.flatMap { socket =>
    IO(active.set(Option(socket))).map(_ => socket)
  }
  private val backoff =
    Stream.eval(IO(log.info(s"Reconnecting to '$url' in $backoffTime..."))).flatMap { _ =>
      Stream.sleep(backoffTime).map(_ => Idle)
    }
  private val untilFailure = allEvents.takeWhile {
    case Failure(_, _, _) => false
    case _                => true
  }
  val events: Stream[IO, SocketEvent] = Stream
    .eval(connectSocket)
    .flatMap(_ => untilFailure ++ backoff)
    .handleErrorWith(t =>
      Stream.eval(IO(log.warn(s"Connection to '$url' failed exceptionally.", t))) >> backoff
    )
    .repeat
    .interruptWhen(interrupter)

  def messagesAs[T: Decoder]: Stream[IO, T] = jsonMessages.flatMap { json =>
    json
      .as[T]
      .fold(
        err => Stream.raiseError(new Exception(s"Failed to decode '$json'.")),
        ok => Stream.emit(ok)
      )
  }

  def sendMessage(message: String): IO[Boolean] = IO(active.get().exists(_.send(message)))

  def close: IO[Unit] =
    IO(log.info(s"Closing socket to '$url'...")) >>
      interrupter.set(true) >>
      IO(active.get().foreach(_.cancel()))

  def requestFor(url: FullUrl, headers: Map[String, String]): Request.Builder =
    headers.foldLeft(new Request.Builder().url(url.url)) {
      case (r, (key, value)) => r.addHeader(key, value)
    }
}
