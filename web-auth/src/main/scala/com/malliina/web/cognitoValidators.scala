package com.malliina.web

import cats.syntax.all.toShow
import com.malliina.values.*

import java.time.Instant

object CognitoValidator extends OAuthKeys:
  val Access = "access"
  val Id = "id"
  val Sub = "sub"
  val TokenUse = "token_use"
  val UserKey = "username"
  val CognitoUserKey = "cognito:username"
  val GroupsKey = "cognito:groups"

case class CognitoValidation(
  issuer: Issuer,
  tokenUse: String,
  clientIdKey: String,
  clientId: ClientId
)

abstract class CognitoValidator[T <: TokenValue, U](keys: Seq[KeyConf], issuer: Issuer)
  extends StaticTokenValidator[T, U](keys, issuer)

class CognitoAccessValidator(keys: Seq[KeyConf], issuer: Issuer, clientId: ClientId)
  extends CognitoValidator[AccessToken, CognitoUser](keys, issuer):
  import com.malliina.web.CognitoValidator.*

  protected def toUser(verified: Verified): Either[JWTError, CognitoUser] =
    val jwt = verified.parsed
    for
      sub <- jwt.parse[CognitoUserId](Sub)
      username <- jwt.parse[Username](UserKey)
      email <- jwt.parseOpt[Email](EmailKey)
      groups <- jwt.readStringListOrEmpty(GroupsKey)
    yield CognitoUser(
      sub,
      username,
      email,
      groups,
      verified
    )

  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    for
      _ <- checkClaim(TokenUse, Access, parsed)
      _ <- checkClaim(ClientIdKey, clientId.show, parsed)
    yield parsed

class CognitoIdValidator(keys: Seq[KeyConf], issuer: Issuer, val clientIds: Seq[ClientId])
  extends CognitoValidator[IdToken, CognitoUser](keys, issuer):
  def this(keys: Seq[KeyConf], issuer: Issuer, clientId: ClientId) =
    this(keys, issuer, Seq(clientId))
  import com.malliina.web.CognitoValidator.*

  override protected def toUser(verified: Verified): Either[JWTError, CognitoUser] =
    val jwt = verified.parsed
    for
      sub <- jwt.parse[CognitoUserId](Sub)
      user <- jwt.parse[Username](CognitoUserKey)
      email <- jwt.parseOpt[Email](EmailKey)
      groups <- jwt.readStringListOrEmpty(GroupsKey)
    yield CognitoUser(sub, user, email, groups, verified)

  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    for
      _ <- checkClaim(TokenUse, Id, parsed)
      _ <- checkContains(Aud, clientIds.map(_.show), parsed)
    yield parsed
