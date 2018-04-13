package com.malliina.http

import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.FunSuite
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class OkClientTests extends FunSuite {
  ignore("can make request") {
    val res = Await.result(OkClient.default.get(FullUrl("http", "www.google.com", "")), 10.seconds)
    assert(res.isSuccess)
  }

  test("parse non-json") {
    val jpe = intercept[JsonParseException] {
      Json.parse("""1: "wrong"""")
    }
    assert(jpe.getMessage.startsWith("Unexpected character"))
  }
}
