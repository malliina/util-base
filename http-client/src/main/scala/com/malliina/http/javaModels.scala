package com.malliina.http

import com.malliina.http.JavaResponseMeta.toMap

import java.net.http.{HttpHeaders as JHttpHeaders, HttpResponse as JHttpResponse}
import java.net.http.HttpResponse.ResponseInfo
import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.JavaConverters.{iterableAsScalaIterableConverter, mapAsScalaMapConverter}

class JavaResponse(inner: JHttpResponse[Array[Byte]]) extends HttpResponse:
  override def charset: Charset = StandardCharsets.UTF_8
  override def body: Array[Byte] = inner.body()
  override def headers: Map[String, Seq[String]] = toMap(inner.headers())
  override def code: Int = inner.statusCode()

class JavaResponseInfo(res: ResponseInfo, val body: Array[Byte]) extends HttpResponse:
  override def charset: Charset = StandardCharsets.UTF_8
  override def headers: Map[String, Seq[String]] = toMap(res.headers())
  override def code: Int = res.statusCode()

object JavaResponseMeta:
  def toMap(hs: JHttpHeaders): Map[String, List[String]] = hs
    .map()
    .asScala
    .map:
      case (k, vs) =>
        k -> vs.asScala.toList
    .toMap

class JavaResponseMeta(inner: ResponseInfo) extends ResponseMeta:
  override def charset: Charset = StandardCharsets.UTF_8
  override def headers: Map[String, Seq[String]] = toMap(inner.headers())
  override def code: Int = inner.statusCode()
