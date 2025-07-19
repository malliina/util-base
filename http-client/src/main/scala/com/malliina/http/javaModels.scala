package com.malliina.http

import java.net.http.{HttpResponse => JHttpResponse}
import java.net.http.HttpResponse.ResponseInfo
import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, mapAsScalaMapConverter}

class StringResponse(inner: JHttpResponse[String]) extends HttpResponse {
  override def asString: String = inner.body()

  override def headers: Map[String, Seq[String]] = inner
    .headers()
    .map()
    .asScala
    .map { case (k, vs) =>
      k -> vs.asScala.toList
    }
    .toMap

  override def code: Int = inner.statusCode()
}

class JavaResponseMeta(inner: ResponseInfo) extends ResponseMeta {
  override def headers: Map[String, Seq[String]] = inner
    .headers()
    .map()
    .asScala
    .map { case (k, vs) =>
      k -> vs.asScala.toList
    }
    .toMap
  override def code: Int = inner.statusCode()
}
