package com.malliina.http

import okhttp3.Response

import scala.collection.JavaConverters.{asScalaBufferConverter, mapAsScalaMapConverter}

object OkHttpResponse {
  def apply(response: Response): OkHttpResponse = new OkHttpResponse(response)
}

class OkHttpResponse(val inner: Response) extends HttpResponse {
  val body = Option(inner.body())
  // Intentionally reads the body eagerly
  val string = body.map(_.string())
  override val asString = string.getOrElse("")

  def code: Int = inner.code()

  def headers: Map[String, Seq[String]] =
    inner.headers().toMultimap.asScala.toMap.map { case (k, v) => k -> v.asScala.toList }
}
