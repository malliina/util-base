package com.malliina.config

import com.malliina.values.Readable

object Env extends ValueReader(sys.env.get)

abstract class ValueReader(readString: String => Option[String]) {
  def readOrElse[T: Readable](key: String, default: => T): Either[InvalidValue, T] =
    read(key).left.flatMap {
      case mv: MissingValue => Right(default)
      case iv: InvalidValue => Left(iv)
    }
  def read[T](key: String)(implicit r: Readable[T]): Either[ConfigError, T] =
    readString(key)
      .toRight(MissingValue(key))
      .flatMap { str =>
        r.read(str).left.map(err => InvalidValue(key, err, None))
      }
}
