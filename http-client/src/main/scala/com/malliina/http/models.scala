package com.malliina.http

import com.malliina.values.StringEnumCompanion
import io.circe.{Decoder, Json, ParsingFailure}
import io.circe.parser.decode

trait ResponseMeta {
  def headers: Map[String, Seq[String]]
  def code: Int
  def status: Int = code
  def isSuccess: Boolean = code >= 200 && code < 300
}

trait HttpResponse extends ResponseMeta {

  /** @return
    *   the body as a string
    */
  def asString: String
  def json: Either[ParsingFailure, Json] = io.circe.parser.parse(asString)
  def parse[T: Decoder]: Either[io.circe.Error, T] = decode[T](asString)
}

sealed trait BodyMethod extends HttpMethod

sealed abstract class HttpMethod(val name: String)

object HttpMethod extends StringEnumCompanion[HttpMethod] {
  override def all: Seq[HttpMethod] = Seq(Get, Post, Put, Patch, Delete, Options, Head)
  override def write(t: HttpMethod) = t.name

  case object Get extends HttpMethod("GET")
  case object Post extends HttpMethod("POST") with BodyMethod
  case object Put extends HttpMethod("PUT") with BodyMethod
  case object Patch extends HttpMethod("PATCH") with BodyMethod
  case object Delete extends HttpMethod("DELETE")
  case object Options extends HttpMethod("OPTIONS")
  case object Head extends HttpMethod("HEAD")
}

sealed trait ResponseError {
  def url: FullUrl
  def response: ResponseMeta
  def code: Int = response.code
  def toException: ResponseException = new ResponseException(this)
}

case class StatusError(response: ResponseMeta, url: FullUrl) extends ResponseError

case class JsonError(error: io.circe.Error, response: ResponseMeta, url: FullUrl)
  extends ResponseError

class ResponseException(val error: ResponseError)
  extends Exception(s"Request to '${error.url}' failed. Status ${error.code}.") {
  def response = error.response
}
