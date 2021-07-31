package com.malliina.http

import cats.effect.IO
import fs2.concurrent.Topic

package object io {
  implicit class TopicOps[T](t: Topic[IO, T]) extends AnyVal {
    def push(message: T): Unit = t.publish1(message).unsafeRunAsyncAndForget()
  }
}
