package com.malliina.http

import com.malliina.values.ErrorMessage
import com.malliina.values.LiteralsSyntax.{LiteralStringContext, getUnsafe}

import scala.quoted.{Expr, Quotes, quotes}

object UrlSyntax extends UrlSyntax

trait UrlSyntax:
  extension (inline ctx: StringContext)
    inline def http(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Http('ctx, 'args) }
    inline def https(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Https('ctx, 'args) }
    inline def wss(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Wss('ctx, 'args) }
    inline def url(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.UrlContext('ctx, 'args) }

private object FullUrlLiterals:
  object Http extends ProtoContext("http")
  object Https extends ProtoContext("https")
  object Wss extends ProtoContext("wss")

  class ProtoContext(proto: String) extends LiteralStringContext[FullUrl]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[FullUrl]] =
      UrlContext.parse(s"$proto://$in")

  object UrlContext extends LiteralStringContext[FullUrl]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[FullUrl]] =
      FullUrl
        .build(in)
        .map: url =>
          '{ FullUrl.build(${ Expr(in) }).getUnsafe }

extension (url: FullUrl)
  def query[Q: KeyValues](q: Q): FullUrl =
    url.query(KeyValues[Q].kvs(q))
