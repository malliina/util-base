package com.malliina.http

import cats.effect.Async

import java.net.http.HttpResponse.{BodyHandler, BodySubscribers, ResponseInfo}
import java.util.concurrent.{CompletableFuture, CompletionStage}

object Ops {
  implicit class BodyHandlerOps[T](bh: BodyHandler[T]) {
    def map[U](f: T => U): BodyHandler[U] = (res: ResponseInfo) =>
      BodySubscribers.mapping(bh(res), t => f(t))
  }
  implicit class CompletionStageOps[T](cf: CompletionStage[T]) {
    def effect[F[_]: Async]: F[T] = Async[F].async_ { cb =>
      cf.whenComplete((r, t) => Option(t).fold(cb(Right(r)))(t => cb(Left(t))))
    }
  }
}
