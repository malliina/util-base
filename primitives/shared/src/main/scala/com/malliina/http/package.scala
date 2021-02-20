package com.malliina

import scala.language.experimental.macros

package object http {
  implicit class LiteralsOps(val sc: StringContext) extends AnyVal {
    def url(args: Any*): FullUrl = macro LiteralMacros.urlInterpolator
  }
}
