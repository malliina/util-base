package com.malliina.values

import scala.annotation.targetName

opaque type NonNeg = Int

object NonNeg extends ValidatedInt[NonNeg]:
  override def build(input: Int): Either[ErrorMessage, NonNeg] =
    if input >= 0 then Right(input)
    else Left(ErrorMessage(s"Value must be non-negative. Got '$input'."))

  override def write(t: NonNeg): Int = t

  def apply(i: Int): Either[ErrorMessage, NonNeg] = build(i)

  extension (nn: NonNeg)
    def value: Int = nn
    def minus(other: Int): Either[ErrorMessage, NonNeg] = apply(value - other)
    def plus(other: Int): Either[ErrorMessage, NonNeg] = apply(value + other)
    @targetName("add")
    def +(other: NonNeg): NonNeg = value + other

/** A trimmed, non-blank string.
  */
opaque type NonBlank <: String = String

object NonBlank extends ValidatedString[NonBlank]:
  override def build(s: String): Either[ErrorMessage, NonBlank] =
    val trimmed = s.trim
    if trimmed.nonEmpty then Right(trimmed)
    else Left(ErrorMessage("Must not be blank."))

  def apply(s: String): Either[ErrorMessage, NonBlank] = build(s)

  override def write(t: NonBlank): String = t

  extension (nb: NonBlank) def append(s: String): NonBlank = s"$nb$s"
