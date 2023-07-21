package com.malliina.config

import com.malliina.http.FullUrl
import com.malliina.values.{ErrorMessage, Readable}
import com.typesafe.config.{Config, ConfigException}

import java.nio.file.{InvalidPathException, Path, Paths}
import scala.util.control.NonFatal

sealed abstract class ConfigError(val message: ErrorMessage, inner: Option[Exception])
  extends Exception(message.message, inner.orNull) {
  def key: String
}
class MissingValue(val key: String) extends ConfigError(ErrorMessage(s"Missing: '$key'."), None)
class InvalidValue(val key: String, message: ErrorMessage, e: Option[Exception])
  extends ConfigError(message, e) {
  def this(key: String, e: Option[Exception]) =
    this(key, ErrorMessage(s"Failed to read '$key'."), e)
}

trait ConfigReadable[T] {
  def read(key: String, c: Config): Either[ErrorMessage, T] =
    parse(key, c).left.map(_.message)
  def parse(key: String, c: Config): Either[ConfigError, T] =
    parseOpt(key, c).flatMap { opt =>
      opt.toRight(new MissingValue(key))
    }
  def parseOpt(key: String, c: Config): Either[InvalidValue, Option[T]]
  def flatMap[U](f: T => ConfigReadable[U]): ConfigReadable[U] = {
    val parent = this
    (key: String, c: Config) =>
      parent
        .parseOpt(key, c)
        .flatMap(opt => opt.map(t => f(t).parseOpt(key, c)).getOrElse(Right(None)))
  }
  def emap[U](f: T => Either[ErrorMessage, U]): ConfigReadable[U] = (key: String, c: Config) =>
    read(key, c)
      .flatMap(t => f(t))
      .fold(err => Left(new InvalidValue(key, err, None)), u => Right(Option(u)))
  def map[U](f: T => U): ConfigReadable[U] = emap(t => Right(f(t)))
}

object ConfigReadable {
  implicit val string: ConfigReadable[String] =
    recovered((key: String, c: Config) => c.getString(key))
  implicit val url: ConfigReadable[FullUrl] =
    string.emap(s => FullUrl.build(s))
  implicit val int: ConfigReadable[Int] =
    recovered((key: String, c: Config) => c.getInt(key))
  implicit val path: ConfigReadable[Path] = ConfigReadable.string.emap { s =>
    try Right(Paths.get(s))
    catch {
      case ipe: InvalidPathException => Left(ErrorMessage(s"Invalid path: '$s'."))
    }
  }
  implicit val bool: ConfigReadable[Boolean] =
    recovered((key: String, c: Config) => c.getBoolean(key))
  implicit val config: ConfigReadable[Config] =
    recovered((key: String, c: Config) => c.getConfig(key))

  private def recovered[T](unsafe: (String, Config) => T): ConfigReadable[T] =
    (key: String, c: Config) =>
      try Right(Option(unsafe(key, c)))
      catch {
        case _: ConfigException.Missing => Right(None)
        case e: Exception =>
          Left(new InvalidValue(key, Option(e)))
      }

  implicit def readable[T](implicit r: Readable[T]): ConfigReadable[T] =
    string.emap(s => r.read(s))
}
