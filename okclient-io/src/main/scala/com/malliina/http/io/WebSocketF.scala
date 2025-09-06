package com.malliina.http.io

import cats.effect.kernel.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, Sync}
import cats.syntax.all._
import com.malliina.http.{FullUrl, OkHttpHttpClient, ReconnectingSocket, SocketBuilder, SocketEvent}
import com.malliina.util.AppLogger
import fs2.concurrent.Topic
import okhttp3._

import java.util.concurrent.atomic.AtomicBoolean

object WebSocketF {
  private val log = AppLogger(getClass)

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    client: OkHttpClient
  ): Resource[F, ReconnectingSocket[F, OkSocket[F]]] =
    for {
      d <- Dispatcher.parallel[F]
      builder = new OkSocketBuilder(url, headers, client, d)
      s <- Resource.eval(ReconnectingSocket.build(builder))
    } yield s

  class OkSocketBuilder[F[_]: Async](
    val url: FullUrl,
    headers: Map[String, String],
    client: OkHttpClient,
    d: Dispatcher[F]
  ) extends SocketBuilder[F, OkSocket[F]] {
    val request = OkHttpHttpClient.requestFor(url, headers).build()
    val interrupted = new AtomicBoolean(false)

    override def connect(sink: Topic[F, SocketEvent]): F[OkSocket[F]] = Sync[F].delay {
      val listener = new OkListener(url, sink, interrupted, d)
      log.info(s"Connecting to '$url'...")
      new OkSocket(client.newWebSocket(request, listener.listener))
    }
  }
}
