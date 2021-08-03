package com.malliina.http.io

import cats.effect.{Concurrent, IO, Timer}
import com.malliina.http.io.ReconnectingSocket.log
import com.malliina.http.io.SocketEvent.Idle
import com.malliina.util.AppLogger
import fs2.Stream
import fs2.concurrent.SignallingRef
import okhttp3.WebSocket

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.DurationInt

object ReconnectingSocket {
  private val log = AppLogger(getClass)

  def apply(initial: WebSocketIO)(implicit t: Timer[IO]): IO[ReconnectingSocket] = {
    implicit val cs = initial.cs
    SignallingRef[IO, Boolean](false).map { i => new ReconnectingSocket(initial, i) }
  }
}

class ReconnectingSocket(specs: WebSocketIO, interrupter: SignallingRef[IO, Boolean])(
  implicit c: Concurrent[IO],
  t: Timer[IO]
) {
  private val backoffTime = 10.seconds
  private val active = new AtomicReference[Option[WebSocket]](None)
  private def connect(): IO[WebSocket] = specs.connect.flatMap { socket =>
    IO(active.set(Option(socket))).map(_ => socket)
  }
  val backoff =
    Stream.eval(IO(log.info(s"Reconnecting to '${specs.url}' in $backoffTime..."))).flatMap { _ =>
      Stream.sleep(backoffTime).map(_ => Idle)
    }
  val events = Stream
    .eval(connect())
    .flatMap(_ => specs.events ++ backoff)
    .repeat
    .interruptWhen(interrupter)
  val messages: Stream[IO, String] = events.collect {
    case SocketEvent.TextMessage(_, message) => message
  }

  def send(message: String): Boolean = active.get().map(_.send(message)).getOrElse(false)

  def close(): Unit = {
    log.info(s"Closing socket to '${specs.url}'...")
    interrupter.set(true).unsafeRunSync()
    active.get().foreach(_.cancel())
  }
}
