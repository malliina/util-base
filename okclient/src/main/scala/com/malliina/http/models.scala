package com.malliina.http

import io.circe._
import io.circe.parser._
import com.malliina.values.StringEnumCompanion
import okhttp3.Response

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
  def json: Either[ParsingFailure, Json] = io.circe.parser.parse(asString)
  def parse[T: Decoder]: Either[io.circe.Error, T] = decode[T](asString)
  def isSuccess: Boolean = code >= 200 && code < 300
}

sealed trait ResponseError {
  def url: FullUrl
  def response: OkHttpResponse
  def code: Int = response.code
  def toException: ResponseException = new ResponseException(this)
}

case class StatusError(response: OkHttpResponse, url: FullUrl) extends ResponseError

case class JsonError(error: io.circe.Error, response: OkHttpResponse, url: FullUrl)
  extends ResponseError

class ResponseException(val error: ResponseError)
  extends Exception(s"Request to '${error.url}' failed. Status ${error.code}.") {
  def response = error.response
}

sealed abstract class HttpMethod(val name: String)

object HttpMethod extends StringEnumCompanion[HttpMethod] {
  override def all: Seq[HttpMethod] = Seq(Get, Post, Put, Patch, Delete, Options, Head)
  override def write(t: HttpMethod) = t.name

  case object Get extends HttpMethod("GET")
  case object Post extends HttpMethod("POST")
  case object Put extends HttpMethod("PUT")
  case object Patch extends HttpMethod("PATCH")
  case object Delete extends HttpMethod("DELETE")
  case object Options extends HttpMethod("OPTIONS")
  case object Head extends HttpMethod("HEAD")
}
