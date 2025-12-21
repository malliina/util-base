package com.malliina.http.io

import cats.effect.Sync
import cats.syntax.all.toFlatMapOps
import com.malliina.http.WebSocketOps
import okhttp3.WebSocket

class OkSocket[F[_]: Sync](impl: WebSocket) extends WebSocketOps[F] {
  private val F = Sync[F]

  override def sendMessage(s: String): F[Boolean] = F.delay(impl.send(s))

  override def trySend(message: String): F[Unit] = sendMessage(message).flatMap { isEnqueued =>
    if (isEnqueued) F.unit
    else F.raiseError(new Exception(s"Failed to send message. Connection closed?"))
  }

  override def closeNow: F[Unit] = F.delay(impl.cancel())
}
