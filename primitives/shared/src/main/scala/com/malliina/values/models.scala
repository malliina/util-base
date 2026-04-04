package com.malliina.values

case class Email private (email: String) extends AnyVal with WrappedString {
  def value: String = email
}
object Email extends StringCompanion[Email] {
  override def build(input: String): Either[ErrorMessage, Email] =
    if (input.contains("@") && input.length >= 3) Right(apply(input))
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
  override def build(input: String): Either[ErrorMessage, Username] =
    if (input.isBlank) Left(ErrorMessage("Username cannot be blank."))
    else Right(apply(input))
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
object AccessToken extends TokenCompanion[AccessToken] {
  override protected def make(token: String): AccessToken = apply(token)
}

case class IdToken private (token: String) extends TokenValue(token)
object IdToken extends TokenCompanion[IdToken] {
  override protected def make(token: String): IdToken = apply(token)
}

case class RefreshToken private (token: String) extends TokenValue(token)
object RefreshToken extends TokenCompanion[RefreshToken] {
  override protected def make(token: String): RefreshToken = apply(token)
}

case class JSONWebToken private (token: String) extends TokenValue(token) {
  def access = AccessToken.fromJwt(this)
  def refresh = RefreshToken.fromJwt(this)
  def id = IdToken.fromJwt(this)
}

object JSONWebToken extends StringCompanion[JSONWebToken] {
  override def build(input: String): Either[ErrorMessage, JSONWebToken] =
    if (input.isBlank) Left(ErrorMessage("JWT cannot be blank."))
    else Right(apply(input))
}

sealed abstract class TokenValue(token: String) extends WrappedString {
  override def value: String = token
  override def toString: String = token
}

sealed abstract class TokenCompanion[T <: WrappedString] extends StringCompanion[T] {
  protected def make(token: String): T

  override def build(input: String): Either[ErrorMessage, T] =
    if (input.isBlank) Left(ErrorMessage("Token cannot be blank."))
    else Right(make(input))

  def fromJwt(jwt: JSONWebToken): T = make(jwt.token)
}

case class ErrorMessage(message: String) extends WrappedString {
  override def value = message
}

object ErrorMessage extends StringCompanion[ErrorMessage] {
  override def build(input: String): Either[ErrorMessage, ErrorMessage] =
    if (input.nonEmpty) Right(ErrorMessage(input))
    else Left(ErrorMessage("Must not be empty."))
}
