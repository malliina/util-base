package com.mle.concurrent

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 * @author mle
 */
object FutureImplicits {

  implicit class RichFuture[T](fut: Future[T]) {
    def recoverAll[U >: T](fix: Throwable => U)(implicit executor: ExecutionContext): Future[U] =
      fut.recover {
        case t: Throwable => fix(t)
      }

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
