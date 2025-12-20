package com.malliina.http

import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource, Sync}
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.Ops.CompletionStageOps
import com.malliina.util.AppLogger
import fs2.concurrent.Topic
import org.typelevel.ci.CIString

import java.net.URI
import java.net.http.{HttpClient => JHttpClient, WebSocket => JWebSocket}
import java.util.concurrent.CompletionStage
import concurrent.duration.{DurationInt, FiniteDuration}

object WebSocket {
  private val log = AppLogger(getClass)

  val DefaultBackOff: FiniteDuration = 10.seconds

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    http: JHttpClient,
    backoffTime: FiniteDuration = DefaultBackOff
  ): Resource[F, ReconnectingSocket[F, JavaSocket[F]]] =
    for {
      d <- Dispatcher.parallel[F]
      builder = new JavaSocketBuilder(url, headers, http, d)
      s <- ReconnectingSocket.resource(builder, backoffTime)
    } yield s

  class JavaSocketBuilder[F[_]: Async](
    val url: FullUrl,
    headers: Map[String, String],
    http: JHttpClient,
    d: Dispatcher[F]
  ) extends SocketBuilder[F, JavaSocket[F]] {
    private def newBuilder = headers.foldLeft(http.newWebSocketBuilder()) { case (c, (k, v)) =>
      c.header(k, v)
    }
    override def connect(
      sink: Topic[F, SocketEvent],
      headers: Map[CIString, String]
    ): F[JavaSocket[F]] = delayF {
      val listener = new JavaSocketListener(sink, d, url)
      log.info(s"Connecting to '$url'...")
      val connBuilder = headers.foldLeft(newBuilder) { case (b, (k, v)) =>
        b.header(k.toString, v)
      }
      connBuilder.buildAsync(URI.create(url.url), listener)
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
