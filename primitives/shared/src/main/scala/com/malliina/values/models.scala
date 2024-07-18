package com.malliina.values

case class Email(email: String) extends AnyVal with WrappedString {
  def value: String = email
}
object Email extends StringCompanion[Email]

case class UserId(id: Long) extends WrappedId
object UserId extends IdCompanion[UserId]

case class Username(name: String) extends AnyVal with WrappedString {
  override def value: String = name
}
object Username extends StringCompanion[Username] {
  val empty = Username("")
}

case class Password(pass: String) extends AnyVal with WrappedString {
  override def value: String = pass
  override def toString: String = "****"
}
object Password extends StringCompanion[Password]

case class AccessToken(token: String) extends TokenValue(token)
object AccessToken extends StringCompanion[AccessToken]

case class IdToken(token: String) extends TokenValue(token)
object IdToken extends StringCompanion[IdToken]

case class RefreshToken(token: String) extends TokenValue(token)
object RefreshToken extends StringCompanion[RefreshToken]

sealed abstract class TokenValue(token: String) extends WrappedString {
  override def value: String = token
  override def toString: String = token
}

case class ErrorMessage(message: String) extends WrappedString {
  override def value = message
}

object ErrorMessage extends StringCompanion[ErrorMessage] {
  override def build(input: String): Either[ErrorMessage, ErrorMessage] =
    if (input.nonEmpty) Right(ErrorMessage(input))
    else Left(ErrorMessage("Must not be empty."))
}
