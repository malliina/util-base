package com.malliina

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

package object util {

  implicit class TryOps[T](orig: Try[T]) {
    def recoverNonFatal[U >: T](fix: Throwable => U): Try[U] = orig.recover {
      case NonFatal(t) => fix(t)
    }

    def recoverAll[U >: T](fix: Throwable => U): Try[U] = orig.recover {
      case NonFatal(t) => fix(t)
    }

    def recoverWithAll[U >: T](fix: Throwable => Try[U]): Try[U] = orig.recoverWith {
      case NonFatal(t) => fix(t)
    }

    def fold[U](ifFailure: Throwable => U)(ifSuccess: T => U): U = orig match {
      case Success(s) => ifSuccess(s)
      case Failure(t) => ifFailure(t)
    }
  }
}
