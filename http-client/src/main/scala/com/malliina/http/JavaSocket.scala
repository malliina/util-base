package com.malliina.http

import cats.effect.Async
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.JavaSocket.log
import com.malliina.http.Ops.CompletionStageOps
import com.malliina.util.AppLogger

import java.net.http.{WebSocket => JWebSocket}
import java.util.concurrent.CompletionStage

object JavaSocket {
  private val log = AppLogger(getClass)
}

class JavaSocket[F[_]: Async](impl: JWebSocket, url: FullUrl) extends WebSocketOps[F] {
  override def sendMessage(s: String): F[Boolean] = delayF(impl.sendText(s, true)).as(true)

  override def closeNow: F[Unit] = delayF {
    log.info(s"Closing socket to '$url'...")
    impl.sendClose(JWebSocket.NORMAL_CLOSURE, "").thenApply(_ => impl.abort())
  }

  private def delayF[A](thunk: => CompletionStage[A]): F[A] =
    Async[F].delay(thunk).flatMap(_.effect[F])
}
