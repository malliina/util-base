package com.mle.util

import scala.util.Try

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
  }

}
