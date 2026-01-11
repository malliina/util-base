package com.malliina.values

import com.malliina.values.LiteralsSyntax.{LiteralInt, LiteralStringContext, getUnsafe}

import scala.quoted.{Expr, Quotes, quotes}

object Literals extends Literals

trait Literals:
  extension (i: Int) inline def nonNeg: NonNeg = ${ Impls.NonNegLiteral('i) }

  extension (inline ctx: StringContext)
    inline def str(inline args: Any*): NonBlank =
      ${ Impls.NonBlankLiteral('ctx, 'args) }
    inline def err(inline args: Any*): ErrorMessage =
      ${ Impls.ErrorMessageLiteral('ctx, 'args) }
    inline def error(args: Any*): ErrorMessage =
      val msg = ctx.s(args*)
      ErrorMessage(msg)
    inline def jwt(inline args: Any*): JSONWebToken =
      ${ Impls.JWTLiteral('ctx, 'args) }
    inline def user(inline args: Any*): Username =
      ${ Impls.UsernameLiteral('ctx, 'args) }
    inline def pass(inline args: Any*): Password =
      ${ Impls.PasswordLiteral('ctx, 'args) }

private object Impls:
  object NonNegLiteral extends LiteralInt[NonNeg]:
    override def parse(in: Int)(using Quotes): Either[ErrorMessage, Expr[NonNeg]] =
      NonNeg(in).map: _ =>
        '{ NonNeg(${ Expr(in) }).getUnsafe }

  object NonBlankLiteral extends LiteralStringContext[NonBlank]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[NonBlank]] =
      NonBlank(in).map: _ =>
        '{ NonBlank(${ Expr(in) }).getUnsafe }

  object ErrorMessageLiteral extends LiteralStringContext[ErrorMessage]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[ErrorMessage]] =
      ErrorMessage
        .build(in)
        .map: _ =>
          '{ ErrorMessage.build(${ Expr(in) }).getUnsafe }

  object JWTLiteral extends LiteralStringContext[JSONWebToken]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[JSONWebToken]] =
      JSONWebToken
        .build(in)
        .map: _ =>
          '{ JSONWebToken.build(${ Expr(in) }).getUnsafe }

  object UsernameLiteral extends LiteralStringContext[Username]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[Username]] =
      Username
        .build(in)
        .map: _ =>
          '{ Username.build(${ Expr(in) }).getUnsafe }

  object PasswordLiteral extends LiteralStringContext[Password]:
    override def parse(in: String)(using Quotes): Either[ErrorMessage, Expr[Password]] =
      Password
        .build(in)
        .map: _ =>
          '{ Password.build(${ Expr(in) }).getUnsafe }

object LiteralsSyntax:
  trait LiteralInt[T]:
    def parse(in: Int)(using Quotes): Either[ErrorMessage, Expr[T]]

    def apply(x: Expr[Int])(using Quotes): Expr[T] =
      val f = x.valueOrAbort
      parse(f)
        .fold(
          err =>
            quotes.reflect.report.error(err.message)
            ???
          ,
          ok => ok
        )

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
