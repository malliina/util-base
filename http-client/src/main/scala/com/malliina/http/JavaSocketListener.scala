package com.malliina.http

import cats.effect.std.Dispatcher
import com.malliina.http.Ops.EffectOps
import com.malliina.http.SocketEvent.{BytesMessage, Failure, TextMessage}
import fs2.concurrent.Topic
import fs2.concurrent.Topic.Closed

import java.net.http.{WebSocket => JWebSocket}
import java.nio.ByteBuffer
import java.util.concurrent.{CompletionStage, Executor}

class JavaSocketListener[F[_]](
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
