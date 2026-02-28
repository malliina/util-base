package com.malliina.http.io

import cats.effect.Sync
import cats.syntax.all.toFlatMapOps
import com.malliina.http.{FullUrl, SendException, WebSocketOps}
import okhttp3.WebSocket

class OkSocket[F[_]: Sync](impl: WebSocket, url: FullUrl) extends WebSocketOps[F] {
  private val F = Sync[F]

  override def sendMessage(s: String): F[Boolean] = F.delay(impl.send(s))

  override def trySend(message: String): F[Unit] = sendMessage(message).flatMap { isEnqueued =>
    if (isEnqueued) F.unit
    else
      F.raiseError(
        new SendException(s"Failed to enqueue '$message' to '$url'. Connection closed?", None)
      )
  }

  override def closeNow: F[Unit] = F.delay(impl.cancel())
}
