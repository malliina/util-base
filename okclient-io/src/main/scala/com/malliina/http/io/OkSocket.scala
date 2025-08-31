package com.malliina.http.io

import cats.effect.Sync
import com.malliina.http.WebSocketOps
import okhttp3.WebSocket

class OkSocket[F[_]: Sync](impl: WebSocket) extends WebSocketOps[F] {
  override def sendMessage(s: String): F[Boolean] = Sync[F].delay(impl.send(s))
  override def closeNow: F[Unit] = Sync[F].delay(impl.cancel())
}
