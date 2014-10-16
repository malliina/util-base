package com.mle.concurrent

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

/**
 * @author Michael
 */
object ExecutionContexts {
  implicit val cached: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}
