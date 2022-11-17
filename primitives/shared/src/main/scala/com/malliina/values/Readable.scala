package com.malliina.values

import scala.util.Try

trait Readable[R] {
  def read(value: String): Either[ErrorMessage, R]
  def map[S](f: R => S): Readable[S] = (s: String) => read(s).map(f)
  def flatMap[S](f: R => Readable[S]): Readable[S] = (s: String) =>
    read(s).flatMap(r => f(r).read(s))
  def emap[S](f: R => Either[ErrorMessage, S]): Readable[S] = (s: String) => read(s).flatMap(f)
}

object Readable {
  implicit val string: Readable[String] = (s: String) => Right(s)
  implicit val email: Readable[Email] = string.map(Email.apply)
  implicit val long: Readable[Long] = string.emap { s =>
    Try(s.toLong).fold(_ => Left(ErrorMessage(s"Invalid long: '$s'.")), l => Right(l))
  }
  implicit val userId: Readable[UserId] = long.emap(UserId.build)
  implicit val int: Readable[Int] = string.emap { s =>
    Try(s.toInt).fold(_ => Left(ErrorMessage(s"Invalid long: '$s'.")), l => Right(l))
  }
  implicit val boolean: Readable[Boolean] = string.emap {
    case "true"  => Right(true)
    case "false" => Right(false)
    case other   => Left(ErrorMessage(s"Invalid boolean: '$other'."))
  }
}
