package com.mle.util

import java.util.concurrent.{ScheduledFuture, TimeUnit, Executors}
import scala.concurrent.duration.Duration

/**
 *
 * @author mle
 */
object Scheduling {
  private val executor = Executors.newSingleThreadScheduledExecutor()

  def every(interval: Duration)(code: => Unit): ScheduledFuture[_] = {
    val intervalMillis = interval.toMillis
    executor.scheduleWithFixedDelay(Utils.runnable(code), 1, intervalMillis, TimeUnit.MILLISECONDS)
  }

  def shutdown() {
    executor.shutdown()
  }
}
