package com.mle.concurrent

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 * @author mle
 */
object FutureImplicits {

  implicit class RichFuture[T](fut: Future[T]) {
    def recoverAll[U >: T](fix: Throwable => U)(implicit ec: ExecutionContext): Future[U] =
      fut.recover {
        case t: Throwable => fix(t)
      }

    def recoverWithAll[U >: T](fix: Throwable => Future[U])(implicit ec: ExecutionContext): Future[U] =
      fut.recoverWith {
        case t: Throwable => fix(t)
      }

    def orElse[U >: T](other: => Future[U])(implicit ec: ExecutionContext) = recoverWithAll(_ => other)

    def exists(predicate: T => Boolean)(implicit ec: ExecutionContext): Future[Boolean] =
      fut.map(predicate).recoverAll(_ => false)

    def isDefined = fut.exists(_ => true)

    /**
     * The following doesn't compile:
     * f.recoverFrom[IllegalArgumentException, Int](iae => 42)
     * Why not?
     */
    //    def recoverFrom[E <: Throwable, U >: T](fix: E => U)(implicit executor: ExecutionContext, manifest: Manifest[E]): Future[U] =
    //      fut.recover {
    //        case e: E => fix(e)
    //      }
  }

}
