package com.malliina.values

case class ErrorMessage(message: String) extends WrappedString {
  override def value = message
}

object ErrorMessage extends StringCompanion[ErrorMessage]
