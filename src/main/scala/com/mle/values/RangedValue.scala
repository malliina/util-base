package com.mle.values

/**
 * @author Michael
 */
abstract class RangedValue[T, U <: WrappedValue[T]](min: T, max: T) extends RangeValidator[T, U] {
  override val Min: T = min
  override val Max: T = max

  override def strip(elem: U): T = elem.value
}