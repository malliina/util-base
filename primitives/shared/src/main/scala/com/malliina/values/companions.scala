package com.malliina.values

import play.api.libs.json._

trait Identifier {
  def id: String

  override def toString: String = id
}

abstract class Wrapped(val value: String) {
  override def toString: String = value
}

trait WrappedId {
  def id: Long

  override def toString = s"$id"
}

abstract class WrappedLong(val num: Long) {
  override def toString = s"$num"
}

abstract class IdentCompanion[T <: Identifier] extends JsonCompanion[String, T] {
  override def write(t: T): String = t.id
}

abstract class IdCompanion[T <: WrappedId] extends JsonCompanion[Long, T] {
  override def write(t: T) = t.id
}

abstract class StringCompanion[T <: Wrapped] extends JsonCompanion[String, T] {
  override def write(t: T) = t.value
}

abstract class JsonCompanion[Raw, T](implicit f: Format[Raw], o: Ordering[Raw])
    extends ValidatingCompanion[Raw, T] {
  def apply(raw: Raw): T

  override def build(input: Raw): Either[ErrorMessage, T] =
    Right(apply(input))
}

abstract class ValidatingCompanion[Raw, T](implicit f: Format[Raw], o: Ordering[Raw]) {
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
  implicit val ordering: Ordering[T] = o.on(write)

  def build(input: Raw): Either[ErrorMessage, T]

  def write(t: T): Raw

  def defaultError(in: Raw): ErrorMessage =
    ErrorMessage(s"Invalid input: '$in'.")
}

abstract class WrappedEnum[T <: Wrapped] extends StringEnumCompanion[T] {
  override def write(t: T) = t.value
}

abstract class StringEnumCompanion[T] extends EnumCompanion[String, T] {
  override def build(input: String): Either[ErrorMessage, T] =
    all.find(i => write(i).toLowerCase == input.toLowerCase).toRight(defaultError(input))
}

abstract class EnumCompanion[Raw, T](implicit f: Format[Raw], o: Ordering[Raw])
    extends ValidatingCompanion[Raw, T] {

  def all: Seq[T]

  def resolveName(item: T): Raw = write(item)

  private def allNames = all.map(write).mkString(", ")

  def build(input: Raw): Either[ErrorMessage, T] =
    all.find(i => write(i) == input).toRight(defaultError(input))

  override def defaultError(input: Raw) =
    ErrorMessage(s"Unknown input: '$input'. Must be one of: $allNames.")
}
