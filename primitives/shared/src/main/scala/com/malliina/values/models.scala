package com.malliina.values

case class Email private (email: String) extends AnyVal with WrappedString {
  def value: String = email
}
object Email extends StringCompanion[Email] {
  override def build(input: String): Either[ErrorMessage, Email] =
    if (input.contains("@")) Right(apply(input))
    else Left(ErrorMessage(s"Invalid email: '$input'."))
}

case class UserId private (id: Long) extends WrappedId
object UserId extends IdCompanion[UserId] {
  override def build(input: Long): Either[ErrorMessage, UserId] =
    Right(apply(input))
}

case class Username private (name: String) extends AnyVal with WrappedString {
  override def value: String = name
}
object Username extends StringCompanion[Username] {
  val empty = Username("")

  override def build(input: String): Either[ErrorMessage, Username] =
    Right(apply(input))
}

case class Password private (pass: String) extends AnyVal with WrappedString {
  override def value: String = pass
  override def toString: String = "****"
}
object Password extends StringCompanion[Password] {
  override def build(input: String): Either[ErrorMessage, Password] =
    if (input.isBlank) Left(ErrorMessage("Password cannot be blank."))
    else Right(apply(input))
}

case class AccessToken private (token: String) extends TokenValue(token)
object AccessToken extends StringCompanion[AccessToken] {
  override def build(input: String): Either[ErrorMessage, AccessToken] =
    if (input.isBlank) Left(ErrorMessage("Access token cannot be blank."))
    else Right(apply(input))
}

case class IdToken private (token: String) extends TokenValue(token)
object IdToken extends StringCompanion[IdToken] {
  override def build(input: String): Either[ErrorMessage, IdToken] =
    if (input.isBlank) Left(ErrorMessage("ID token cannot be blank."))
    else Right(apply(input))
}

case class RefreshToken private (token: String) extends TokenValue(token)
object RefreshToken extends StringCompanion[RefreshToken] {
  override def build(input: String): Either[ErrorMessage, RefreshToken] =
    if (input.isBlank) Left(ErrorMessage("Refresh token cannot be blank."))
    else Right(apply(input))
}

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
