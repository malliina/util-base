package com.malliina.values

import io.circe._, io.circe.generic.auto._
import scala.util.{Try, Failure, Success}

trait Identifier extends Any {
  def id: String
  override def toString: String = id
}

@deprecated("Use WrappedString instead.", "1.10.0")
abstract class Wrapped(val value: String) {
  override def toString: String = value
}

trait WrappedString extends Any {
  def value: String
  override def toString: String = value
}

trait WrappedId extends Any {
  def id: Long
  override def toString = s"$id"
}

trait WrappedLong extends Any {
  def num: Long
  override def toString = s"$num"
}

trait WrappedValue[T] {
  def value: T
}

abstract class IdentCompanion[T <: Identifier] extends JsonCompanion[String, T] {
  override def write(t: T): String = t.id
}

abstract class IdCompanion[T <: WrappedId] extends JsonCompanion[Long, T] {
  override def write(t: T): Long = t.id
}

abstract class StringCompanion[T <: WrappedString] extends JsonCompanion[String, T] {
  implicit val readable: Readable[T] = Readable.string.flatMap(s => build(s))

  override def write(t: T): String = t.value
}

abstract class JsonCompanion[Raw, T](implicit d: Decoder[Raw], e: Encoder[Raw], o: Ordering[Raw])
  extends ValidatingCompanion[Raw, T] {
  def apply(raw: Raw): T

  override def build(input: Raw): Either[ErrorMessage, T] =
    Right(apply(input))
}

abstract class ValidatingCompanion[Raw, T](
  implicit d: Decoder[Raw],
  e: Encoder[Raw],
  o: Ordering[Raw]
) {
  implicit val json: Codec[T] = Codec.from(
    d.emap(raw => build(raw).left.map(err => err.message)),
    e.contramap[T](write)
  )
  implicit val ordering: Ordering[T] = o.on(write)

  def build(input: Raw): Either[ErrorMessage, T]
  def write(t: T): Raw
  def defaultError(in: Raw): ErrorMessage = ErrorMessage(s"Invalid input: '$in'.")
}

abstract class WrappedEnum[T <: WrappedString] extends StringEnumCompanion[T] {
  override def write(t: T) = t.value
}

abstract class StringEnumCompanion[T] extends EnumCompanion[String, T] {
  override def build(input: String): Either[ErrorMessage, T] =
    all.find(i => write(i).toLowerCase == input.toLowerCase).toRight(defaultError(input))
}

abstract class EnumCompanion[Raw, T](implicit f: Decoder[Raw], e: Encoder[Raw], o: Ordering[Raw])
  extends ValidatingCompanion[Raw, T] {

  def all: Seq[T]
  def resolveName(item: T): Raw = write(item)
  private def allNames = all.map(write).mkString(", ")

  def build(input: Raw): Either[ErrorMessage, T] =
    all.find(i => write(i) == input).toRight(defaultError(input))

  override def defaultError(input: Raw) =
    ErrorMessage(s"Unknown input: '$input'. Must be one of: $allNames.")
}
