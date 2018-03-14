package com.malliina.http

import okhttp3.Response
import play.api.libs.json.{JsError, JsValue, Json, Reads}

import scala.util.Try

class OkHttpResponse(val inner: Response) extends HttpResponse {
  override val asString = inner.body().string()

  def code: Int = inner.code()
}

trait HttpResponse {
  /**
    * @return the body as a string
    */
  def asString: String

  def code: Int

  def json: Either[JsError, JsValue] =
    Try(Json.parse(asString)).toOption.toRight(JsError(s"Not JSON: '$asString'."))

  def parse[T: Reads]: Either[JsError, T] =
    json.flatMap(_.validate[T].asEither.left.map(err => JsError(err)))


  def isSuccess = code >= 200 && code < 400
}

trait ResponseError

case class StatusError(response: OkHttpResponse, url: FullUrl) extends ResponseError {
  def code = response.code
}

case class JsonError(error: JsError, response: OkHttpResponse, url: FullUrl) extends ResponseError
