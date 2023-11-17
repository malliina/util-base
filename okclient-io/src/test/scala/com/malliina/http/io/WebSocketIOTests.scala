package com.malliina.http.io

import cats.effect.IO
import cats.effect.kernel.Temporal
import munit.FunSuite
import cats.effect.unsafe.implicits.global
import fs2.concurrent.Topic
import fs2.Stream
import concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}

class WebSocketIOTests extends FunSuite {
  test("topic example") {
    val list: Vector[String] = Topic[IO, String].flatMap { topic =>
      val publisher = Stream.constant("1").covary[IO].through(topic.publish)
      val subscriber = topic.subscribe(10).take(4)
      subscriber.concurrently(publisher).compile.toVector
    }
      .unsafeRunSync()
    assertEquals(list, Vector.fill(4)("1"))
  }
}
