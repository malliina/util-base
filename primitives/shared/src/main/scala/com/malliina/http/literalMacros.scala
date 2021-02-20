package com.malliina.http

import com.malliina.values.ErrorMessage

import scala.reflect.macros.blackbox

trait LiteralSyntax {
  implicit def urlLiteralsSyntax(sc: StringContext): LiteralsOps =
    new LiteralsOps(sc)
}

object LiteralMacros {
  def urlInterpolator(c: blackbox.Context)(args: c.Expr[Any]*): c.Expr[FullUrl] =
    singlePartInterpolator(c)(
      args,
      s => FullUrl.build(s),
      s =>
        c.universe.reify(
          FullUrl.build(s.splice).fold(err => throw new Exception(err.message), identity)
        )
    )

  // Adapted from http4s macro code
  private def singlePartInterpolator[A](c: blackbox.Context)(
    args: Seq[c.Expr[Any]],
    validate: String => Either[ErrorMessage, A],
    construct: c.Expr[String] => c.Expr[A]
  ): c.Expr[A] = {
    import c.universe._
    identity(args)
    c.prefix.tree match {
      case Apply(_, List(Apply(_, (lcp @ Literal(Constant(p: String))) :: Nil))) =>
        validate(p).fold(
          error => c.abort(c.enclosingPosition, error.message),
          ok => construct(c.Expr(lcp))
        )
    }
  }
}
