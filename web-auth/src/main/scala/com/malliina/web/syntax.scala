package com.malliina.web

import com.malliina.values.LiteralsSyntax.LiteralStringContext
import com.malliina.values.{ErrorMessage, getUnsafe}

import scala.quoted.{Expr, Quotes}

object WebLiterals extends WebLiterals

trait WebLiterals:
  extension (inline ctx: StringContext)
    inline def issuer(inline args: Any*): Issuer =
      ${ Impls.IssuerLiteral('ctx, 'args) }

private object Impls:
  object IssuerLiteral extends LiteralStringContext[Issuer]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[Issuer]] =
      Issuer
        .build(in)
        .map: _ =>
          '{ Issuer.build(${ Expr(in) }).getUnsafe }
