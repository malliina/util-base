package com.malliina.http

import cats.effect.IO
import fs2.concurrent.Topic
import cats.effect.unsafe.implicits.global

package object io {
  implicit class TopicOps[T](val t: Topic[IO, T]) extends AnyVal {
    def push(message: T): Unit = t.publish1(message).unsafeRunAndForget()
  }
}
