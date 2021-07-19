package com.malliina.values

trait IntValidator[T] extends RangeValidator[Int, T] {
  override def isValid(elem: Int): Boolean = elem >= Min && elem <= Max
}
