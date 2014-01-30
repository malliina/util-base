package com.mle.concurrent

import scala.concurrent._
import scala.concurrent.duration.Duration
import com.mle.util.Utils

/**
 *
 * @author mle
 */
trait Futures {
  /**
   * Fails `f` with a [[TimeoutException]] unless it's completed within `timeout`.
   *
   * @param timeout duration
   * @param f future computation to wrap in a timeout
   * @tparam T type of result
   * @return a future which may time out
   */
  def within[T](timeout: Duration)(f: Future[T])(implicit executor: ExecutionContext): Future[T] =
    promisedFuture[T](p => {
      f.onComplete(result => if (!p.isCompleted) p.tryComplete(result))
      delay(timeout).onComplete(_ => {
        if (!p.isCompleted)
          p.tryFailure(new TimeoutException(s"Unable to complete task within $timeout."))
      })
    })

  /**
   * Applies `test` to each element of `in` and returns the first successful
   * result of `test` along with the element.
   *
   * If all tests fail, the returned [[Future]] fails with a [[NoSuchElementException]].
   * Specific failures of individual [[Future]]s are ignored. The returned [[Future]] fails
   * with a [[TimeoutException]] if there's no success within `timeout`.
   *
   * @param in elements under test
   * @param test the test
   * @tparam T type of element
   * @tparam U type of result
   * @return the element that first successfully completes the test along with its test result
   */
  def firstResult[T, U](in: Seq[T], timeout: Duration = Duration.Inf)(test: T => Future[U])(implicit executor: ExecutionContext): Future[(T, U)] =
    within[(T, U)](timeout)(promisedFuture[(T, U)](p => {
      // uses the suppresser to make the Future returned from Future.sequence resilient to individual failures
      val attempts = in map (elem => test(elem) map (r => if (!p.isCompleted) p.trySuccess((elem, r))) recover Utils.suppresser)
      Future.sequence(attempts).onComplete {
        case _ if !p.isCompleted => p tryFailure new NoSuchElementException
      }
    }))

  def firstSuccessful[T, U](in: Seq[T], timeout: Duration = Duration.Inf)(test: T => Future[U])(implicit executor: ExecutionContext): Future[T] =
    firstResult(in, timeout)(test).map(_._1)

  /**
   * This method blocks a thread until `dur` has passed, but does not block the caller thread.
   *
   * @param dur length of duration
   * @return a [[Future]] that completes successfully after `dur` has passed
   */
  def delay(dur: Duration)(implicit executor: ExecutionContext): Future[Unit] = Future {
    blocking {
      Thread.sleep(dur.toMillis)
    }
  }

  /**
   * Constructs a future that is completed according to `keepPromise`. This pattern
   * can be used to convert callback-based APIs to Future-based ones. For example,
   * parameter `keepPromise` can call some callback-based API, and the callback
   * implementation can complete the supplied promise.
   *
   * @param keepPromise code that completes the promise
   * @tparam T type of value to complete promise with
   * @return the future completion value
   */
  def promisedFuture[T](keepPromise: Promise[T] => Unit): Future[T] = {
    val p = promise[T]()
    keepPromise(p)
    p.future
  }
}

object Futures extends Futures