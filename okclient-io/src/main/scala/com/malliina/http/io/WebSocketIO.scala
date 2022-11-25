package com.malliina.http.io

import cats.effect.{Async, Concurrent, IO, Sync}
import cats.effect.kernel.{Resource, Temporal}
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.http.io.SocketEvent.*
import com.malliina.http.io.WebSocketF.log
import com.malliina.util.AppLogger
import fs2.{RaiseThrowable, Stream}
import fs2.concurrent.{SignallingRef, Topic}
import io.circe.*
import io.circe.syntax.EncoderOps
import okhttp3.*
import okio.ByteString

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait WebSocketOps[F[_]] {
  def send[T: Encoder](message: T): F[Boolean] = sendMessage(message.asJson.noSpaces)
  def sendMessage(s: String): F[Boolean]
}

object WebSocketF {
  private val log = AppLogger(getClass)

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    client: OkHttpClient
  ): Resource[F, WebSocketF[F]] =
    for {
      topic <- Resource.eval(Topic[F, SocketEvent])
      interrupter <- Resource.eval(SignallingRef[F, Boolean](false))
      d <- Dispatcher[F]
      socket <- Resource.make(
        Sync[F].delay(new WebSocketF(url, headers, client, topic, interrupter, d))
      )(s => s.close)
    } yield socket
}

class WebSocketF[F[_]: Async](
  val url: FullUrl,
  headers: Map[String, String],
  client: OkHttpClient,
  topic: Topic[F, SocketEvent],
  interrupter: SignallingRef[F, Boolean],
  d: Dispatcher[F]
) extends WebSocketOps[F] {
  private val backoffTime: FiniteDuration = 10.seconds
  private val active: AtomicReference[Option[WebSocket]] =
    new AtomicReference[Option[WebSocket]](None)

  val allEvents: Stream[F, SocketEvent] = topic.subscribe(10)
  val messages: Stream[F, String] = allEvents.collect {
    case TextMessage(_, message) => message
  }
  val jsonMessages: Stream[F, Json] = messages.flatMap { message =>
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
  val connectOnce: F[WebSocket] =
    delay(log.info(s"Connecting to '$url'...")) >> delay(client.newWebSocket(request, listener))
  val connectSocket: F[WebSocket] = connectOnce.flatMap { socket =>
    delay(active.set(Option(socket))).map(_ => socket)
  }
  private val backoff =
    Stream.eval(delay(log.info(s"Reconnecting to '$url' in $backoffTime..."))).flatMap { _ =>
      Stream.sleep(backoffTime).map(_ => Idle)
    }
  private val untilFailure: Stream[F, SocketEvent] = allEvents.takeWhile {
    case Failure(_, _, _) => false
    case _                => true
  }
  private val eventsOrFailure: Stream[F, SocketEvent] = allEvents.flatMap {
    case f @ Failure(_, t, _) =>
      val logging = delay {
        t.map { ex => log.warn(s"Connection to '$url' failed exceptionally.", ex) }.getOrElse {
          log.warn("Connection to '$url' failed.")
        }
      }
      Stream.eval(logging) >> Stream.raiseError(f.exception)
    case f @ Closed(_, code, reason) =>
      Stream.eval(delay(log.warn(s"Socket to '$url' closed with code $code reason '$reason'."))) >>
        Stream.raiseError(f.exception)
    case other =>
      Stream.emit(other)
  }
  val connections: Stream[F, List[SocketEvent]] = Stream.retry(
    Stream
      .eval(connectSocket)
      .flatMap(_ => eventsOrFailure)
      .compile
      .toList,
    backoffTime,
    delay => delay * 2,
    maxAttempts = 100000
  )
  val events: Stream[F, SocketEvent] = connections
    .flatMap(xs => Stream.emits(xs))
    .interruptWhen(interrupter)
  val eventsConstantBackoff: Stream[F, SocketEvent] = Stream
    .eval(connectSocket)
    .flatMap(_ => untilFailure ++ backoff)
    .handleErrorWith(t =>
      Stream.eval(delay(log.warn(s"Connection to '$url' failed exceptionally.", t))) >> backoff
    )
    .repeat
    .interruptWhen(interrupter)

  def messagesAs[T: Decoder]: Stream[F, T] = jsonMessages.flatMap { json =>
    json
      .as[T]
      .fold(
        err => Stream.raiseError(new Exception(s"Failed to decode '$json'.")),
        ok => Stream.emit(ok)
      )
  }

  def sendMessage(message: String): F[Boolean] = delay(active.get().exists(_.send(message)))

  def close: F[Unit] =
    delay(log.info(s"Closing socket to '$url'...")) >>
      interrupter.set(true) >>
      delay(active.get().foreach(_.cancel()))

  def requestFor(url: FullUrl, headers: Map[String, String]): Request.Builder =
    HttpClient.requestFor(url, headers)

  private def delay[A](thunk: => A) = Sync[F].delay(thunk)
}
