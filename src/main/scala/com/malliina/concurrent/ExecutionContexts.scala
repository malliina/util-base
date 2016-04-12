package com.malliina.concurrent

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

object ExecutionContexts {
  implicit val cached: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}
