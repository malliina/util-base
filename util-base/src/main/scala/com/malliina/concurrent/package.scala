package com.malliina

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

package object concurrent {

  implicit class FutureOps[T](fut: Future[T]) {
    def recoverAll[U >: T](fix: Throwable => U)(implicit ec: ExecutionContext): Future[U] =
      fut.recover {
        case NonFatal(t) => fix(t)
      }

    def recoverWithAll[U >: T](fix: Throwable => Future[U])(implicit ec: ExecutionContext): Future[U] =
      fut.recoverWith {
        case NonFatal(t) => fix(t)
      }

    def orElse[U >: T](other: => Future[U])(implicit ec: ExecutionContext) = recoverWithAll(_ => other)

    def exists(predicate: T => Boolean)(implicit ec: ExecutionContext): Future[Boolean] =
      fut.map(predicate).recoverAll(_ => false)

    def isDefined(implicit ec: ExecutionContext) = fut.exists(_ => true)
  }

  implicit class PromiseOps[T](p: Promise[T]) {
    /** Tries to fail this [[Promise]] with a [[scala.concurrent.TimeoutException]] after `to`.
      *
      * @param to timeout
      */
    def timeout(to: Duration) = Futures.timeoutAfter(to, p)

    /**
      *
      * @return a new [[Promise]]
      */
    def withTimeout(to: Duration): Promise[T] = {
      val newPromise = Promise[T]()
      p.future.onComplete(newPromise.tryComplete)(ExecutionContext.Implicits.global)
      newPromise timeout to
      newPromise
    }
  }

}
