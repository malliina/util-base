package com.mle.values

/**
 * @author Michael
 */
trait DoubleValidator[T] extends RangeValidator[Double, T] {
  override def isValid(elem: Double): Boolean = elem >= Min && elem <= Max
}