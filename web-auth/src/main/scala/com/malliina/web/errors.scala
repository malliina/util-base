package com.malliina.web

import com.malliina.http.{ResponseError, ResponseException, StatusError}
import com.malliina.values.{ErrorMessage, TokenValue}

import java.text.ParseException
import java.time.Instant
import scala.concurrent.duration.{Duration, DurationLong}

sealed abstract class AuthError(val key: String):
  def message: ErrorMessage

case class OkError(error: ResponseError) extends AuthError(OkError.Key):
  override def message: ErrorMessage = ErrorMessage(error match
    case StatusError(r, _)                    => s"Status code ${r.code}."
    case com.malliina.http.JsonError(_, _, _) => "JSON error.")

object OkError:
  val Key = "http_error"
  def apply(e: ResponseException): OkError = apply(e.error)

case class PermissionError(message: ErrorMessage) extends AuthError(PermissionError.Key)

object PermissionError:
  val Key = "permission_error"

case class OAuthError(message: ErrorMessage) extends AuthError(OAuthError.Key)

object OAuthError:
  val Key = "oauth_error"
  def apply(s: String): OAuthError = OAuthError(ErrorMessage(s))

case class JsonError(err: String) extends AuthError(JsonError.Key):
  override def message = ErrorMessage(s"JSON error. $err")

object JsonError:
  val Key = "json_error"

sealed abstract class JWTError(key: String) extends AuthError(key):
  def token: TokenValue
  def message: ErrorMessage

case class Expired(token: TokenValue, exp: Instant, now: Instant) extends JWTError(Expired.Key):
  def since: Duration = (now.toEpochMilli - exp.toEpochMilli).millis
  override def message = ErrorMessage(s"Token expired $since ago, at $exp.")

object Expired:
  val Key = "token_expired"

case class NotYetValid(token: TokenValue, nbf: Instant, now: Instant)
  extends JWTError(NotYetValid.Key):
  def validIn = (nbf.toEpochMilli - now.toEpochMilli).millis

  override def message = ErrorMessage(
    s"Token not yet valid. Valid in $validIn. Valid from $nbf, checked at $now."
  )

object NotYetValid:
  val Key = "not_yet_valid"

case class IssuerMismatch(token: TokenValue, actual: Issuer, allowed: Seq[Issuer])
  extends JWTError(IssuerMismatch.Key):
  def message = ErrorMessage(
    s"Issuer mismatch. Got '$actual', but expected one of '${allowed.mkString(", ")}'."
  )

object IssuerMismatch:
  val Key = "issuer_mismatch"

case class InvalidSignature(token: TokenValue) extends JWTError(InvalidSignature.Key):
  override def message = ErrorMessage("Invalid JWT signature.")

object InvalidSignature:
  val Key = "invalid_signature"

case class InvalidKeyId(token: TokenValue, kid: String, expected: Seq[String])
  extends JWTError(InvalidKeyId.Key):
  def message = ErrorMessage(
    s"Invalid key ID. Expected one of '${expected.mkString(", ")}', but got '$kid'."
  )

object InvalidKeyId:
  val Key = "invalid_kid"

case class InvalidClaims(token: TokenValue, message: ErrorMessage)
  extends JWTError(InvalidClaims.Key)

object InvalidClaims:
  val Key = "invalid_claims"

case class ParseError(token: TokenValue, e: Option[ParseException], message: ErrorMessage)
  extends JWTError(ParseError.Key)

object ParseError:
  val Key = "parse_error"

  def exception(token: TokenValue, e: ParseException) =
    apply(token, Option(e), ErrorMessage("Parse error."))
  def simple(token: TokenValue, message: ErrorMessage) =
    apply(token, None, message)

case class MissingData(token: TokenValue, message: ErrorMessage) extends JWTError(MissingData.Key)

object MissingData:
  val Key = "missing_data"
