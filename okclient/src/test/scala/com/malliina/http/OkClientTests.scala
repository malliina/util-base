package com.malliina.http

import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class OkClientTests extends FunSuite {
  ignore("can make request") {
    val res = Await.result(OkClient.default.get(FullUrl("http", "www.google.com", "")), 10.seconds)
    assert(res.isSuccess)
  }
}
