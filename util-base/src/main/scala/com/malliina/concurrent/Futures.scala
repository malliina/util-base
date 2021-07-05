package com.malliina.concurrent

import com.malliina.util.Utils

import scala.concurrent._
import scala.concurrent.duration.Duration

trait Futures {
  def before[T, TO](other: Future[TO])(f: Future[T])(implicit ec: ExecutionContext): Future[T] =
    promisedFuture[T](p => {
      f.onComplete(result => if (!p.isCompleted) p.tryComplete(result))
      other.onComplete(_ => {
        if (!p.isCompleted)
          p.tryFailure(
            new TimeoutException(s"Unable to complete task within the given time limit.")
          )
      })
    })

  /** Constructs a future that is completed according to `keepPromise`. This pattern
    * can be used to convert callback-based APIs to Future-based ones. For example,
    * parameter `keepPromise` can call some callback-based API, and the callback
    * implementation can complete the supplied promise.
    *
    * @param keepPromise code that completes the promise
    * @tparam T type of value to complete promise with
    * @return the future completion value
    */
  def promisedFuture[T](keepPromise: Promise[T] => Unit): Future[T] = {
    val p = Promise[T]()
    keepPromise(p)
    p.future
  }
}

object Futures extends Futures
