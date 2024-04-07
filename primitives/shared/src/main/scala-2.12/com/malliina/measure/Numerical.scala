package com.malliina.measure

object Numerical {
  def apply[Raw, T](read: Raw => T, write: T => Raw)(implicit raw: Numeric[Raw]): Numeric[T] =
    new Numeric[T] {
      override def plus(x: T, y: T): T = read(raw.plus(write(x), write(y)))
      override def minus(x: T, y: T): T = read(raw.minus(write(x), write(y)))
      override def times(x: T, y: T): T = read(raw.times(write(x), write(y)))
      override def negate(x: T): T = read(raw.negate(write(x)))
      override def fromInt(x: Int): T = read(raw.fromInt(x))
      override def toInt(x: T): Int = raw.toInt(write(x))
      override def toLong(x: T): Long = raw.toLong(write(x))
      override def toFloat(x: T): Float = raw.toFloat(write(x))
      override def toDouble(x: T): Double = raw.toDouble(write(x))
      override def compare(x: T, y: T): Int = raw.compare(write(x), write(y))
    }
}
