package com.malliina.values

trait Readable[R] {
  def read(key: String): Either[ErrorMessage, R]
  def map[S](f: R => S): Readable[S] = (s: String) => read(s).map(f)
  def flatMap[S](f: R => Readable[S]): Readable[S] = (s: String) =>
    read(s).flatMap(r => f(r).read(s))
  def emap[S](f: R => Either[ErrorMessage, S]): Readable[S] = (s: String) => read(s).flatMap(f)
}

object Readable {
  implicit val string: Readable[String] = (s: String) => Right(s)
  implicit val email: Readable[Email] = string.map(Email.apply)
}
