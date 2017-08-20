package com.malliina.values

case class ErrorMessage(message: String) extends Wrapped(message)

object ErrorMessage extends StringCompanion[ErrorMessage]
