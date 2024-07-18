package com.malliina.http

import com.malliina.values.ErrorMessage
import com.malliina.values.LiteralsSyntax.{LiteralStringContext, getUnsafe}

import scala.quoted.{Expr, Quotes, quotes}

object UrlSyntax extends UrlSyntax

trait UrlSyntax:
  extension (inline ctx: StringContext)
    inline def https(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Https('ctx, 'args) }
    inline def wss(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Wss('ctx, 'args) }

private object FullUrlLiterals:
  object Https extends UrlContext("https")
  object Wss extends UrlContext("wss")

  class UrlContext(proto: String) extends LiteralStringContext[FullUrl]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[FullUrl]] =
      val str = s"$proto://$in"
      FullUrl
        .build(str)
        .map: url =>
          '{ FullUrl.build(${ Expr(str) }).getUnsafe }
