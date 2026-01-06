package com.malliina.values

import cats.Show

opaque type Showable <: String = String

object Showable:
  given fromShow[T: Show]: Conversion[T, Showable] with
    def apply(t: T): Showable = Show[T].show(t)
