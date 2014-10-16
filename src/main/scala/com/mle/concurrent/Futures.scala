package com.mle.concurrent

import com.mle.util.Utils
import rx.lang.scala.Observable

import scala.concurrent._
import scala.concurrent.duration.Duration

/**
 *
 * @author mle
 */
trait Futures {
  def unit[T](elem: T) = Future successful elem

  /**
   * Fails `f` with a [[TimeoutException]] unless it's completed within `timeout`.
   *
   * @param timeout duration
   * @param f future computation to wrap in a timeout
   * @tparam T type of result
   * @return a future which may time out
   */
  def within[T](timeout: Duration)(f: Future[T])(implicit executor: ExecutionContext): Future[T] =
    before(delay2(timeout))(f)

  def before[T, TO](other: Future[TO])(f: Future[T])(implicit executor: ExecutionContext): Future[T] =
    promisedFuture[T](p => {
      f.onComplete(result => if (!p.isCompleted) p.tryComplete(result))
      other.onComplete(_ => {
        if (!p.isCompleted)
          p.tryFailure(new TimeoutException(s"Unable to complete task within the given time limit."))
      })
    })

  /**
   * Applies `test` to each element of `in` and returns the first element that completes the test successfully along
   * with its result.
   *
   * If all tests fail, the returned [[Future]] fails with a [[NoSuchElementException]]. Failures of individual
   * [[Future]]s are ignored.
   *
   * @param in elements under test
   * @param test the test
   * @tparam T type of element
   * @tparam U type of result
   * @return the element that first successfully completes the test along with its test result
   */
  def firstResult[T, U](in: Seq[T])(test: T => Future[U])(implicit executor: ExecutionContext): Future[(T, U)] =
    promisedFuture[(T, U)](p => {
      // uses the suppresser to make the Future returned from Future.sequence resilient to individual failures
      val attempts = in map (elem => test(elem) map (r => if (!p.isCompleted) p.trySuccess((elem, r))) recover Utils.suppresser)
      Future.sequence(attempts).onComplete {
        case _ if !p.isCompleted => p tryFailure new NoSuchElementException
      }
    })

  def firstSuccessful[T, U](in: Seq[T])(test: T => Future[U])(implicit executor: ExecutionContext): Future[T] =
    firstResult(in)(test).map(_._1)

  def firstSuccessful[T, U](in: Seq[T], timeout: Duration)(test: T => Future[U])(implicit executor: ExecutionContext): Future[T] =
    within(timeout)(firstSuccessful(in)(test))

  /**
   * This method blocks a thread until `dur` has passed, but does not block the caller thread.
   *
   * @param dur length of duration
   * @return a [[Future]] that completes successfully after `dur` has passed
   */
  @scala.deprecated("Use `delay2(Duration)` instead.")
  def delay(dur: Duration)(implicit executor: ExecutionContext): Future[Unit] =
    Future(blocking(Thread.sleep(dur.toMillis)))

  def delay2(dur: Duration): Future[Unit] = after(dur)(())

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
    val p = Promise[T]()
    keepPromise(p)
    p.future
  }

  def after[T](duration: Duration)(code: => T): Future[T] = {
    val p = Promise[T]()
    lazy val codeEval = code
    val sub = observeAfter(duration).subscribe(_ => p trySuccess codeEval)
    val ret = p.future
    ret.onComplete(_ => sub.unsubscribe())(ExecutionContext.Implicits.global)
    ret
  }

  /**
   * Emits 0 and completes after `duration`.
   *
   * @param duration
   * @return a one-item [[Observable]]
   */
  def observeAfter(duration: Duration) = Observable.interval(duration).take(1)

  def timeoutAfter[T](duration: Duration, promise: Promise[T]) =
    after(duration)(promise tryFailure new concurrent.TimeoutException(s"Timed out after $duration."))

  /**
   *
   * @param duration timeout
   * @tparam T type of promise
   * @return a [[Promise]] that fails with a [[TimeoutException]] after `duration` has passed unless completed by then
   */
  def timedPromise[T](duration: Duration) = {
    val p = Promise[T]()
    timeoutAfter(duration, p)
    p
  }
}

object Futures extends Futures