package com.mle.util

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.reflect.Manifest
import scala.util.Try

/**
 *
 * @author mle
 */
object Utils {
  implicit val executionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(50))

  val suppresser: PartialFunction[Throwable, Unit] = {
    case _: Throwable => ()
  }

  def runnable(f: => Any): Runnable = new Runnable {
    def run() {
      f
    }
  }
}
