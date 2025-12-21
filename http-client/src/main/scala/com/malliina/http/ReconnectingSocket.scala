package com.malliina.http

import cats.effect.{Async, Ref, Resource, Sync}
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.all.{catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}
import com.malliina.http.ReconnectingSocket.log
import com.malliina.http.SocketEvent.{Closed, Failure, TextMessage}
import com.malliina.util.AppLogger
import fs2.Stream
import fs2.concurrent.{SignallingRef, Topic}
import io.circe.{Decoder, Json, parser}
import org.typelevel.ci.{CIString, CIStringSyntax}

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.FiniteDuration

object ReconnectingSocket {
  private val log = AppLogger(getClass)

  val `X-Connection` = ci"X-Connection"

  def resource[F[_]: Async, S <: WebSocketOps[F]](
    builder: SocketBuilder[F, S],
    backoffTime: FiniteDuration = WebSocket.DefaultBackOff
  ): Resource[F, ReconnectingSocket[F, S]] = {
    val make = for {
      topic <- Topic[F, SocketEvent]
      ref <- Ref.of[F, Option[S]](None)
      conns <- Ref.of[F, Int](0)
      interrupter <- SignallingRef[F, Boolean](false)
    } yield new ReconnectingSocket[F, S](topic, ref, conns, interrupter, builder, backoffTime)
    Resource.make(make)(_.close)
  }
}

class ReconnectingSocket[F[_]: Async, S <: WebSocketOps[F]](
  topic: Topic[F, SocketEvent],
  active: Ref[F, Option[S]],
  connections: Ref[F, Int],
  interrupter: SignallingRef[F, Boolean],
  builder: SocketBuilder[F, S],
  backoffTime: FiniteDuration = WebSocket.DefaultBackOff
) extends WebSocketOps[F] {
  val F = Sync[F]
  def url = builder.url
  private val interrupted = new AtomicBoolean(false)

  def connectOnce: F[S] =
    connections.updateAndGet(_ + 1).flatMap { attempt =>
      builder.connect(topic, Map(ReconnectingSocket.`X-Connection` -> s"$attempt")).onError {
        case connError =>
          delay(log.warn(s"Failed to connect to ${builder.url}.", connError))
      }
    }

  val connectSocket: F[S] = connectOnce.flatMap { socket =>
    active.set(Option(socket)).map(_ => socket)
  }

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
  private val eventsOrFailure: Stream[F, SocketEvent] = allEvents.flatMap {
    case f @ Failure(_, t) =>
      val logging = delay {
        t.map(ex => log.warn(s"Connection to '$url' failed exceptionally.", ex)).getOrElse {
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
          eventsOrFailure
            .evalMap(ev => receiver.publish1(ev))
            .concurrently(Stream.eval(connectSocket))
            .compile
            .drain,
          backoffTime,
          delay => delay * 2,
          maxAttempts = 100000
        )
      receiver.subscribe(10).concurrently(consume)
    }
    .interruptWhen(interrupter)

  def messagesAs[T: Decoder]: Stream[F, T] = jsonMessages.flatMap { json =>
    json
      .as[T]
      .fold(
        err => Stream.raiseError(new Exception(s"Failed to decode '$json'.")),
        ok => Stream.emit(ok)
      )
  }

  override def sendMessage(s: String): F[Boolean] =
    trySend(s).as(true).handleError(_ => false)

  override def trySend(message: String): F[Unit] =
    active.get.flatMap { opt =>
      opt
        .map(a => a.trySend(message))
        .getOrElse(
          F.raiseError(new Exception(s"Unable to send message to '$url'. No active socket."))
        )
    }

  override def closeNow: F[Unit] = close

  def close: F[Unit] =
    delay(log.info(s"Closing socket to '$url'...")) >>
      delay(interrupted.set(true)) >>
      interrupter.set(true) >>
      active.get.flatMap(opt => opt.map(_.closeNow).getOrElse(F.pure(())))

  private def delay[A](thunk: => A) = F.delay(thunk)
}
