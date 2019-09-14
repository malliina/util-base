package com.malliina.http

import okhttp3.Response
import play.api.libs.json.{JsError, JsValue, Json, Reads}

import scala.collection.JavaConverters.{asScalaBufferConverter, mapAsScalaMapConverter}
import scala.util.Try

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

trait HttpResponse {

  /**
    * @return the body as a string
    */
  def asString: String
  def headers: Map[String, Seq[String]]
  def code: Int
  def status: Int = code

  def json: Either[JsError, JsValue] =
    Try(Json.parse(asString)).toOption.toRight(JsError(s"Not JSON: '$asString'."))

  def parse[T: Reads]: Either[JsError, T] =
    json.flatMap(_.validate[T].asEither.left.map(err => JsError(err)))

  def isSuccess: Boolean = code >= 200 && code < 300
}

sealed trait ResponseError {
  def url: FullUrl
  def response: OkHttpResponse
  def code: Int = response.code
  def toException: ResponseException = new ResponseException(this)
}

case class StatusError(response: OkHttpResponse, url: FullUrl) extends ResponseError

case class JsonError(error: JsError, response: OkHttpResponse, url: FullUrl) extends ResponseError

class ResponseException(val error: ResponseError)
    extends Exception(s"Request to '${error.url}' failed. Status ${error.code}.") {
  def response = error.response
}
