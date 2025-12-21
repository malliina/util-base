package com.malliina.http

import cats.effect.Async
import cats.implicits.catsSyntaxApplicativeError
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
  override def sendMessage(s: String): F[Boolean] =
    trySend(s).as(true).handleError(_ => false)

  override def trySend(message: String): F[Unit] =
    delayF(impl.sendText(message, true)).void

  override def closeNow: F[Unit] = delayF {
    log.info(s"Closing socket to '$url'...")
    impl.sendClose(JWebSocket.NORMAL_CLOSURE, "").thenApply(_ => impl.abort())
  }

  private def delayF[A](thunk: => CompletionStage[A]): F[A] =
    Async[F].delay(thunk).flatMap(_.effect[F])
}
