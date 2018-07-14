package com.malliina.values

case class Email(email: String) extends Wrapped(email)

object Email extends StringCompanion[Email]

case class UserId(id: Long) extends WrappedId

object UserId extends IdCompanion[UserId]

case class Username(name: String) extends Wrapped(name)

object Username extends StringCompanion[Username] {
  val empty = Username("")
}

case class Password(pass: String) extends Wrapped(pass) {
  override def toString: String = "****"
}

object Password extends StringCompanion[Password]

case class AccessToken(token: String) extends TokenValue(token)

object AccessToken extends StringCompanion[AccessToken]

case class IdToken(token: String) extends TokenValue(token)

object IdToken extends StringCompanion[IdToken]

case class RefreshToken(token: String) extends TokenValue(token)

object RefreshToken extends StringCompanion[RefreshToken]

sealed abstract class TokenValue(token: String) extends Wrapped(token) {
  override def toString = token
}
