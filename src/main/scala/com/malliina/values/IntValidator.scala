package com.malliina.values

/**
 * @author Michael
 */
trait IntValidator[T] extends RangeValidator[Int, T] {
  implicit val json = jsonFormat

  override def isValid(elem: Int): Boolean = elem >= Min && elem <= Max
}
