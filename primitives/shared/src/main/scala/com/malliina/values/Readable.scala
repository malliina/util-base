package com.malliina.values

import com.malliina.http.FullUrl
import com.malliina.measure.{DistanceM, SpeedM, Temperature}
import com.malliina.storage.StorageSize

import scala.util.Try

trait Readable[R] {
  def read(value: String): Either[ErrorMessage, R]
  def map[S](f: R => S): Readable[S] = (s: String) => read(s).map(f)
  def flatMap[S](f: R => Readable[S]): Readable[S] = (s: String) =>
    read(s).flatMap(r => f(r).read(s))
  def emap[S](f: R => Either[ErrorMessage, S]): Readable[S] = (s: String) => read(s).flatMap(f)
}

object Readable {
  def apply[T](implicit r: Readable[T]): Readable[T] = r
  implicit val string: Readable[String] = (s: String) => Right(s)
  implicit val int: Readable[Int] = fromTry("integer", s => Try(s.toInt))
  implicit val long: Readable[Long] = fromTry("long", s => Try(s.toLong))
  implicit val float: Readable[Float] = fromTry("float", s => Try(s.toFloat))
  implicit val double: Readable[Double] = fromTry("double", s => Try(s.toDouble))
  implicit val distance: Readable[DistanceM] = double.map(DistanceM.apply)
  implicit val speed: Readable[SpeedM] = double.map(SpeedM.apply)
  implicit val temperature: Readable[Temperature] = double.map(Temperature.apply)
  implicit val storageSize: Readable[StorageSize] = long.map(StorageSize.apply)
  implicit val boolean: Readable[Boolean] = string.emap {
    case "true"  => Right(true)
    case "false" => Right(false)
    case other   => Left(ErrorMessage(s"Invalid boolean: '$other'."))
  }
  implicit val url: Readable[FullUrl] = string.emap(FullUrl.build)

  private def fromTry[T](label: String, t: String => Try[T]) =
    string.emap(s => t(s).fold(_ => Left(ErrorMessage(s"Invalid $label: '$s'.")), v => Right(v)))
}
