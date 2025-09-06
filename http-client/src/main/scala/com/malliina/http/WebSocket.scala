package com.malliina.http

import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.Ops.CompletionStageOps
import com.malliina.util.AppLogger
import fs2.concurrent.Topic

import java.net.URI
import java.net.http.{HttpClient => JHttpClient, WebSocket => JWebSocket}
import java.util.concurrent.CompletionStage

object WebSocket {
  private val log = AppLogger(getClass)

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    http: JHttpClient
  ): Resource[F, ReconnectingSocket[F, JavaSocket[F]]] = {
    val b = headers.foldLeft(http.newWebSocketBuilder()) { case (c, (k, v)) => c.header(k, v) }
    for {
      d <- Dispatcher.parallel[F]
      builder = new JavaSocketBuilder(url, headers, http, d)
      s <- Resource.eval(ReconnectingSocket.build(builder))
    } yield s
  }

  class JavaSocketBuilder[F[_]: Async](
    val url: FullUrl,
    headers: Map[String, String],
    http: JHttpClient,
    d: Dispatcher[F]
  ) extends SocketBuilder[F, JavaSocket[F]] {
    private val b = headers.foldLeft(http.newWebSocketBuilder()) { case (c, (k, v)) =>
      c.header(k, v)
    }
    override def connect(sink: Topic[F, SocketEvent]): F[JavaSocket[F]] = delayF {
      val listener = new JavaSocketListener(sink, d, url)
      log.info(s"Connecting to '$url'...")
      b.buildAsync(URI.create(url.url), listener)
    }.map(s => new JavaSocket(s, url))
  }

  private def delayF[F[_]: Async, A](thunk: => CompletionStage[A]): F[A] =
    Sync[F].delay(thunk).flatMap(_.effect[F])
}

class WebSocket[F[_]: Async](
  url: FullUrl,
  listener: JavaSocketListener[F],
  builder: JWebSocket.Builder
) {}
