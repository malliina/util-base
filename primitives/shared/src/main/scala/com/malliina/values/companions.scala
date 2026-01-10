package com.malliina.values

import cats.Show
import io.circe.{Codec, Decoder, Encoder}

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

abstract class IdentCompanion[T <: Identifier] extends ValidatingCompanion[String, T] {
  override def write(t: T): String = t.id
}

abstract class IdCompanion[T <: WrappedId] extends ValidatingCompanion[Long, T] {
  override def write(t: T): Long = t.id
}

abstract class StringCompanion[T <: WrappedString] extends ValidatingCompanion[String, T] {
  override def write(t: T): String = t.value
}

abstract class ValidatedLong[T] extends ValidatingCompanion[Long, T]

abstract class ValidatedString[T] extends ValidatingCompanion[String, T]

abstract class ValidatingCompanion[Raw, T](implicit
  d: Decoder[Raw],
  e: Encoder[Raw],
  o: Ordering[Raw],
  r: Readable[Raw],
  s: Show[Raw]
) {
  implicit val json: Codec[T] = Codec.from(
    d.emap(raw => build(raw).left.map(err => err.message)),
    e.contramap[T](write)
  )
  implicit val ordering: Ordering[T] = o.on(write)
  implicit val readable: Readable[T] = r.emap(build)
  implicit val show: Show[T] = Show.show[T](t => s.show(write(t)))
  def build(input: Raw): Either[ErrorMessage, T]
  def unsafe(input: Raw): T =
    build(input).fold(err => throw new IllegalArgumentException(err.message), identity)
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

abstract class EnumCompanion[Raw, T](implicit
  f: Decoder[Raw],
  e: Encoder[Raw],
  o: Ordering[Raw],
  r: Readable[Raw],
  s: Show[Raw]
) extends ValidatingCompanion[Raw, T] {
  def all: Seq[T]
  def resolveName(item: T): Raw = write(item)
  private def allNames = all.map(write).mkString(", ")

  def build(input: Raw): Either[ErrorMessage, T] =
    all.find(i => write(i) == input).toRight(defaultError(input))

  override def defaultError(input: Raw) =
    ErrorMessage(s"Unknown input: '$input'. Must be one of: $allNames.")
}
