package com.malliina.http.io

import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, Sync}
import cats.syntax.all._
import com.malliina.http.{FullUrl, OkHttpHttpClient, ReconnectingSocket, SocketBuilder, SocketEvent}
import com.malliina.util.AppLogger
import fs2.concurrent.Topic
import okhttp3._
import org.typelevel.ci.CIString

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.FiniteDuration

object WebSocketF {
  private val log = AppLogger(getClass)

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    client: OkHttpClient,
    backoffTime: FiniteDuration = com.malliina.http.WebSocket.DefaultBackOff
  ): Resource[F, ReconnectingSocket[F, OkSocket[F]]] =
    for {
      d <- Dispatcher.parallel[F]
      builder = new OkSocketBuilder(url, headers, client, d)
      s <- ReconnectingSocket.resource(builder)
    } yield s

  class OkSocketBuilder[F[_]: Async](
    val url: FullUrl,
    headers: Map[String, String],
    client: OkHttpClient,
    d: Dispatcher[F]
  ) extends SocketBuilder[F, OkSocket[F]] {
    private val builder = OkHttpHttpClient.requestFor(url, headers)
    val interrupted = new AtomicBoolean(false)

    override def connect(
      sink: Topic[F, SocketEvent],
      headers: Map[CIString, String]
    ): F[OkSocket[F]] = Sync[F].delay {
      val listener = new OkListener(url, sink, interrupted, d)
      log.info(s"Connecting to '$url'...")
      val connBuilder = headers.foldLeft(builder) { case (b, (k, v)) =>
        b.addHeader(k.toString, v)
      }
      new OkSocket(client.newWebSocket(connBuilder.build(), listener.listener))
    }
  }
}
