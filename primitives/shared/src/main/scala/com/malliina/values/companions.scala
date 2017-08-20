package com.malliina.values

import play.api.libs.json._

trait Identifier {
  def id: String

  override def toString: String = id
}

abstract class Wrapped(val value: String) {
  override def toString: String = value
}

abstract class WrappedLong(val num: Long) {
  override def toString = s"$num"
}

abstract class IdentCompanion[T <: Identifier] extends JsonCompanion[String, T] {
  override def write(t: T): String = t.id
}

abstract class StringCompanion[T <: Wrapped] extends JsonCompanion[String, T] {
  override def write(t: T) = t.value
}

abstract class JsonCompanion[Raw: Format, T] extends ValidatingCompanion[Raw, T] {
  def apply(raw: Raw): T

  override def build(input: Raw): Either[ErrorMessage, T] =
    Right(apply(input))
}

abstract class ValidatingCompanion[Raw: Format, T] {
  private val reader = Reads[T] { jsValue =>
    jsValue.validate[Raw].flatMap { raw =>
      build(raw).fold(
        error => JsError(error.message),
        t => JsSuccess(t)
      )
    }
  }

  private val writer = Writes[T](t => Json.toJson(write(t)))
  implicit val json = Format[T](reader, writer)

  def build(input: Raw): Either[ErrorMessage, T]

  def write(t: T): Raw

  def defaultError(in: Raw): ErrorMessage = ErrorMessage(s"Invalid input: '$in'.")
}
