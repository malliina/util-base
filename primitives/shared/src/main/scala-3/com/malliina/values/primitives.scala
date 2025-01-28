package com.malliina.values

import io.circe.{Codec, Decoder, Encoder}

import scala.annotation.targetName

opaque type NonNeg = Int

object NonNeg:
  given Codec[NonNeg] = Codec.from(
    Decoder.decodeInt.emap(i => apply(i).left.map(_.message)),
    Encoder.encodeInt.contramap(identity)
  )

  def apply(i: Int): Either[ErrorMessage, NonNeg] =
    if i >= 0 then Right(i)
    else Left(ErrorMessage(s"Value must be non-negative. Got '$i'."))

  extension (nn: NonNeg)
    def value: Int = nn
    def minus(other: Int): Either[ErrorMessage, NonNeg] = apply(value - other)
    def plus(other: Int): Either[ErrorMessage, NonNeg] = apply(value + other)
    @targetName("add")
    def +(other: NonNeg): NonNeg = value + other

/** A trimmed, non-blank string.
  */
opaque type NonBlank <: String = String

object NonBlank:
  def apply(s: String): Either[ErrorMessage, NonBlank] =
    val trimmed = s.trim
    if trimmed.nonEmpty then Right(trimmed)
    else Left(ErrorMessage("Must not be blank."))

  extension (nb: NonBlank) def append(s: String): NonBlank = s"$nb$s"

  given Codec[NonBlank] = Codec.from(
    Decoder.decodeString.emap(i => apply(i).left.map(_.message)),
    Encoder.encodeString.contramap(identity)
  )
