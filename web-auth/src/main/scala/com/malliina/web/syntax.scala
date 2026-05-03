package com.malliina.web

import com.malliina.values.LiteralsSyntax.LiteralStringContext
import com.malliina.values.{ErrorMessage, getUnsafe}

import scala.quoted.{Expr, Quotes}

object WebLiterals extends WebLiterals

trait WebLiterals:
  extension (inline ctx: StringContext)
    inline def issuer(inline args: Any*): Issuer =
      ${ Impls.IssuerLiteral('ctx, 'args) }

    inline def cid(inline args: Any*): ClientId =
      ${ Impls.ClientIdLiteral('ctx, 'args) }

private object Impls:
  object IssuerLiteral extends LiteralStringContext[Issuer]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[Issuer]] =
      Issuer
        .build(in)
        .map: _ =>
          '{ Issuer.build(${ Expr(in) }).getUnsafe }

  object ClientIdLiteral extends LiteralStringContext[ClientId]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[ClientId]] =
      ClientId
        .build(in)
        .map: _ =>
          '{ ClientId.build(${ Expr(in) }).getUnsafe }
