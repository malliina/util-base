package com.malliina.values

trait RangeValidator[T, U] extends ValueValidator[T, U] {
  def empty = build(Default)
  lazy val MinValue = build(Min)
  lazy val MaxValue = build(Max)
  def Default: T = Min
  def Min: T
  def Max: T

//  def encoder[T](implicit e: Encoder[T]): Encoder[U] =
//    e.contramap(t => strip(t))
//
//  def decoder[T](implicit d: Decoder[T]): Decoder[U] =
//    d.emapTry(t =>
//      from(t)
//        .map(u => Success(u))
//        .getOrElse(Failure(new Exception(s"Value out of range: $t, must be within: [$Min, $Max]")))
//    )
}
