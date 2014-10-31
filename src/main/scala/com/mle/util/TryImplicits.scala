package com.mle.util

import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
object TryImplicits {

  implicit class RichTry[T](orig: Try[T]) {
    def recoverAll[U >: T](fix: Throwable => U) = orig.recover {
      case t: Throwable => fix(t)
    }

    def recoverWithAll[U >: T](fix: Throwable => Try[U]) = orig.recoverWith {
      case t: Throwable => fix(t)
    }

    def fold[U](ifFailure: Throwable => U)(ifSuccess: T => U): U = orig match {
      case Success(s) => ifSuccess(s)
      case Failure(t) => ifFailure(t)
    }
  }

}
