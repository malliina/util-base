package com.malliina.concurrent

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

/** For orderly shutdowns I think it's good to shutdown this instance explicitly on app termination.
  */
object ExecutionContexts {
  implicit val cached: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}
