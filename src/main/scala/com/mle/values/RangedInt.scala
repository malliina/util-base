package com.mle.values

/**
 * @author Michael
 */
abstract class RangedInt[U <: WrappedValue[Int]](min: Int, max: Int)
  extends RangedValue[Int, U](min, max)
  with IntValidator[U]
