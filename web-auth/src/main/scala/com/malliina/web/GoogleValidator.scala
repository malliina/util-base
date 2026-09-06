package com.malliina.web

import cats.syntax.all.toShow
import com.malliina.web.WebLiterals.issuer

import java.time.Instant

object GoogleValidator:
  val issuers: Seq[Issuer] = Seq(issuer"https://accounts.google.com", issuer"accounts.google.com")

  def apply(clientIds: Seq[ClientId]): GoogleValidator = new GoogleValidator(clientIds, issuers)

class GoogleValidator(clientIds: Seq[ClientId], issuers: Seq[Issuer])
  extends TokenValidator(issuers):
  override protected def validateClaims(
    parsed: ParsedJWT,
    now: Instant
  ): Either[JWTError, ParsedJWT] =
    checkContains(Aud, clientIds.map(_.show), parsed).map(_ => parsed)
