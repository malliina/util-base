package com.malliina.http.io

import cats.effect.{Async, Sync}
import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.syntax.all._
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.http.io.SocketEvent._
import com.malliina.http.io.WebSocketF.log
import com.malliina.util.AppLogger
import fs2.Stream
import fs2.concurrent.{SignallingRef, Topic}
import io.circe._
import io.circe.syntax.EncoderOps
import okhttp3._
import okio.ByteString

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Success

trait WebSocketOps[F[_]] {
  def send[T: Encoder](message: T): F[Boolean] = sendMessage(message.asJson.noSpaces)
  def sendMessage(s: String): F[Boolean]
}

object WebSocketF {
  private val log = AppLogger(getClass)

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    client: OkHttpClient,
    backoffTime: FiniteDuration = 10.seconds
  ): Resource[F, WebSocketF[F]] =
    for {
      topic <- Resource.eval(Topic[F, SocketEvent])
      interrupter <- Resource.eval(SignallingRef[F, Boolean](false))
      d <- Dispatcher.parallel[F]
      socket <- Resource.make(
        Sync[F].delay(new WebSocketF(url, headers, backoffTime, client, topic, interrupter, d))
      )(s => s.close)
    } yield socket
}

class WebSocketF[F[_]: Async](
  val url: FullUrl,
  headers: Map[String, String],
  backoffTime: FiniteDuration,
  client: OkHttpClient,
  topic: Topic[F, SocketEvent],
  interrupter: SignallingRef[F, Boolean],
  d: Dispatcher[F]
) extends WebSocketOps[F] {
  private val active: AtomicReference[Option[WebSocket]] =
    new AtomicReference[Option[WebSocket]](None)
  private val interrupted = new AtomicBoolean(false)
  val allEvents: Stream[F, SocketEvent] = topic.subscribe(10)
  val messages: Stream[F, String] = allEvents.collect { case TextMessage(_, message) =>
    message
  }
  val jsonMessages: Stream[F, Json] = messages.flatMap { message =>
    parser
      .parse(message)
      .fold(
        err => Stream.raiseError(new Exception(s"Not JSON: '$message'.")),
        ok => Stream.emit(ok)
      )
  }

  private def publish(e: SocketEvent): Unit = {
    val writeLog: (String, Throwable) => Unit =
      if (interrupted.get()) log.debug else log.warn
    implicit val parasitic: ExecutionContext = new ExecutionContext {
      def execute(runnable: Runnable): Unit = runnable.run()
      def reportFailure(t: Throwable): Unit = writeLog(s"Failed to execute.", t)
    }
    d.unsafeToFuture(topic.publish1(e)).onComplete {
      case util.Failure(exception) =>
        writeLog(s"Failed to publish message to '$url'.", exception)
      case Success(value) =>
        value match {
          case Left(value)  => log.warn(s"Failed to publish message to '$url', topic closed.")
          case Right(value) => ()
        }
    }
  }

  private val listener: WebSocketListener = new WebSocketListener {
    override def onClosed(webSocket: WebSocket, code: Int, reason: String): Unit = {
      log.info(s"Closed  socket to '$url'.")
      publish(Closed(webSocket, code, reason))
    }
    override def onClosing(webSocket: WebSocket, code: Int, reason: String): Unit = {
      log.info(s"Closing socket to '$url'.")
      publish(Closing(webSocket, code, reason))
    }
    override def onFailure(webSocket: WebSocket, t: Throwable, response: Response): Unit = {
      if (!interrupted.get())
        log.warn(s"Socket to '$url' failed.", t)
      publish(Failure(webSocket, Option(t), Option(response)))
    }
    override def onMessage(webSocket: WebSocket, text: String): Unit = {
      log.debug(s"Received text '$text'.")
      publish(TextMessage(webSocket, text))
    }
    override def onMessage(webSocket: WebSocket, bytes: ByteString): Unit = {
      log.debug(s"Received bytes $bytes")
      publish(BytesMessage(webSocket, bytes))
    }
    override def onOpen(webSocket: WebSocket, response: Response): Unit = {
      log.info(s"Opened socket to '$url'.")
      publish(Open(webSocket, response))
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
          log.warn(s"Connection to '$url' failed.")
        }
      }
      Stream.eval(logging) >> Stream.raiseError(f.exception)
    case f @ Closed(_, code, reason) =>
      Stream.eval(delay(log.warn(s"Socket to '$url' closed with code $code reason '$reason'."))) >>
        Stream.raiseError(f.exception)
    case other =>
      Stream.emit(other)
  }

  /** Connects to the source, retries on failures with exponential backoff, and returns any
    * non-failure events.
    *
    * Run `close` to interrupt.
    */
  val events = Stream
    .eval(Topic[F, SocketEvent])
    .flatMap { receiver =>
      val consume = Stream
        .retry(
          for {
            socket <- connectSocket
            _ <- eventsOrFailure.evalMap(ev => receiver.publish1(ev)).compile.drain
          } yield socket,
          backoffTime,
          delay => delay * 2,
          maxAttempts = 100000
        )
      receiver.subscribe(10).concurrently(consume)
    }
    .interruptWhen(interrupter)

  /** Use `events` instead.
    */
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
      delay(interrupted.set(true)) >>
      interrupter.set(true) >>
      delay(active.get().foreach(_.cancel()))

  def requestFor(url: FullUrl, headers: Map[String, String]): Request.Builder =
    HttpClient.requestFor(url, headers)

  private def delay[A](thunk: => A) = Sync[F].delay(thunk)
}
