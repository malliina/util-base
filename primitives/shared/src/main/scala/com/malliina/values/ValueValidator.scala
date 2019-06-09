package com.malliina.values

trait ValueValidator[T, U] {
  protected def build(t: T): U

  def isValid(elem: T): Boolean

  def from(elem: T): Option[U] = if (isValid(elem)) Option(build(elem)) else None

  def strip(elem: U): T
}
