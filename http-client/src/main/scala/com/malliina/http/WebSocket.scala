package com.malliina.http

import cats.effect.{Async, Resource}
import cats.syntax.all.toFunctorOps
import com.malliina.http.Ops.CompletionStageOps
import fs2.concurrent.Topic

import java.net.URI
import java.net.http.{HttpClient as JHttpClient, WebSocket as JWebSocket}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletableFuture, CompletionStage, Executor}

object WebSocket {
  private class CompletionOps[F[_], A](fa: F[A]) {
    def toJava: CompletionStage[A] = fa
  }
  class TopicListener[F[_]](topic: Topic[F, SocketEvent], url: FullUrl, executor: Executor)
    extends JWebSocket.Listener {
    override def onOpen(webSocket: JWebSocket): Unit = {
      topic.publish1(SocketEvent.Open(url))
      super.onOpen(webSocket)
    }

    override def onText(
      webSocket: JWebSocket,
      data: CharSequence,
      last: Boolean
    ): CompletionStage[?] =
      CompletableFuture.supplyAsync(???, executor).thenCompose(_ => super.onText(webSocket, data, last))

    override def onBinary(
      webSocket: JWebSocket,
      data: ByteBuffer,
      last: Boolean
    ): CompletionStage[?] =
      super.onBinary(webSocket, data, last)

    override def onPing(webSocket: JWebSocket, message: ByteBuffer): CompletionStage[?] =
      super.onPing(webSocket, message)

    override def onPong(webSocket: JWebSocket, message: ByteBuffer): CompletionStage[?] =
      super.onPong(webSocket, message)

    override def onClose(
      webSocket: JWebSocket,
      statusCode: Int,
      reason: String
    ): CompletionStage[?] =
      super.onClose(webSocket, statusCode, reason)

    override def onError(webSocket: JWebSocket, error: Throwable): Unit =
      super.onError(webSocket, error)
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
      listener = TopicListener(topic, url, executor)
      socket <- Resource.make(
        b.buildAsync(URI.create(url.url), listener).effect[F].map(s => WebSocket[F](s))
      )(s => s.close)
    } yield socket
  }
}

class WebSocket[F[_]](inner: JWebSocket) {
  def close: F[Unit] = ???
}
