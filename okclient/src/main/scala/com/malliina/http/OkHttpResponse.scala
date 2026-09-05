package com.malliina.http

import okhttp3.Response

import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.JavaConverters.{asScalaBufferConverter, mapAsScalaMapConverter}

object OkHttpResponse:
  def apply(response: Response): OkHttpResponse = new OkHttpResponse(response)

class OkHttpResponse(val inner: Response) extends HttpResponse:
  private val innerBody = Option(inner.body())
  override val charset: Charset =
    innerBody.map(_.contentType().charset(StandardCharsets.UTF_8)).getOrElse(StandardCharsets.UTF_8)
  override val body: Array[Byte] = innerBody.map(_.bytes()).getOrElse(Array.emptyByteArray)
  // Intentionally reads the body eagerly
  val string = new String(body, charset)

  def code: Int = inner.code()

  def headers: Map[String, Seq[String]] =
    inner
      .headers()
      .toMultimap
      .asScala
      .toMap
      .map:
        case (k, v) => k -> v.asScala.toList
