package com.malliina.http

import com.malliina.values.ErrorMessage

import scala.quoted.{Expr, Quotes, quotes}

object UrlSyntax extends UrlSyntax

trait UrlSyntax:
  extension (inline ctx: StringContext)
    inline def https(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Https('ctx, 'args) }
    inline def wss(inline args: Any*): FullUrl =
      ${ FullUrlLiterals.Wss('ctx, 'args) }

private object FullUrlLiterals {
  object Https extends UrlContext("https")
  object Wss extends UrlContext("wss")

  class UrlContext(proto: String) extends LiteralStringContext[FullUrl]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[FullUrl]] =
      val str = s"$proto://$in"
      FullUrl.build(str).map { url =>
        '{ FullUrl.build(${ Expr(str) }).getUnsafe }
      }
}

trait LiteralStringContext[T]:
  def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[T]]

  def apply(x: Expr[StringContext], argsExpr: Expr[Seq[Any]])(using Quotes): Expr[T] =
    val parts = x.valueOrAbort.parts
    if parts.size == 1 then
      parse(parts.head).fold(
        err =>
          quotes.reflect.report.error(err.message)
          ???
        ,
        ok => ok
      )
    else
      quotes.reflect.report.error("interpolation not supported", argsExpr)
      ???

extension [T](e: Either[ErrorMessage, T])
  def getUnsafe: T = e.fold(err => throw IllegalArgumentException(err.message), identity)
