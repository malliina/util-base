package com.malliina.http

import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.syntax.all.toFunctorOps
import com.malliina.http.Ops.CompletionStageOps
import com.malliina.http.SocketEvent.{BytesMessage, Failure, TextMessage}
import fs2.concurrent.Topic
import fs2.concurrent.Topic.Closed

import java.net.URI
import java.net.http.{HttpClient => JHttpClient, WebSocket => JWebSocket}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletionStage, Executor}

object WebSocket {
  implicit class CompletionOps[F[_], A](fa: F[A]) {
    def toFuture(d: Dispatcher[F]): CompletionStage[A] =
      d.unsafeToCompletableFuture(fa)
    def await(d: Dispatcher[F]): A =
      d.unsafeRunSync(fa)
  }
  private class TopicListener[F[_]](
    topic: Topic[F, SocketEvent],
    d: Dispatcher[F],
    url: FullUrl,
    executor: Executor
  ) extends JWebSocket.Listener {
    override def onOpen(webSocket: JWebSocket): Unit = {
      publishSync(SocketEvent.Open(url))
      super.onOpen(webSocket)
    }

    override def onText(
      webSocket: JWebSocket,
      data: CharSequence,
      last: Boolean
    ): CompletionStage[?] =
      publish(TextMessage(url, data.toString))
        .thenCompose(_ => super.onText(webSocket, data, last))

    override def onBinary(
      webSocket: JWebSocket,
      data: ByteBuffer,
      last: Boolean
    ): CompletionStage[?] =
      publish(BytesMessage(url, data.array()))
        .thenCompose(_ => super.onBinary(webSocket, data, last))

    override def onPing(webSocket: JWebSocket, message: ByteBuffer): CompletionStage[?] =
      super.onPing(webSocket, message)

    override def onPong(webSocket: JWebSocket, message: ByteBuffer): CompletionStage[?] =
      super.onPong(webSocket, message)

    override def onClose(
      webSocket: JWebSocket,
      statusCode: Int,
      reason: String
    ): CompletionStage[?] =
      publish(SocketEvent.Closed(url, statusCode, reason))
        .thenCompose(_ => super.onClose(webSocket, statusCode, reason))

    override def onError(webSocket: JWebSocket, error: Throwable): Unit = {
      publishSync(Failure(url, Option(error)))
      super.onError(webSocket, error)
    }

    private def publish(e: SocketEvent): CompletionStage[Either[Closed, Unit]] =
      topic.publish1(e).toFuture(d)

    private def publishSync(e: SocketEvent): Either[Closed, Unit] =
      topic.publish1(e).await(d)
  }

  def build[F[_]: Async](
    url: FullUrl,
    headers: Map[String, String],
    http: JHttpClient,
    executor: Executor
  ): Resource[F, WebSocket[F]] = {
    val b = headers.foldLeft(http.newWebSocketBuilder()) { case (c, (k, v)) => c.header(k, v) }
    for {
      topic <- Resource.eval(Topic[F, SocketEvent])
      d <- Dispatcher.parallel[F]
      listener = TopicListener(topic, d, url, executor)
      socket <- Resource.make(
        b.buildAsync(URI.create(url.url), listener).effect[F].map(s => WebSocket[F](s))
      )(s => s.close)
    } yield socket
  }
}

class WebSocket[F[_]](inner: JWebSocket) {
  def close: F[Unit] = ???
}
