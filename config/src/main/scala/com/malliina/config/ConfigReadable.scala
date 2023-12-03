package com.malliina.config

import cats.data.NonEmptyList
import com.malliina.config.ConfigReadable.append
import com.malliina.values.{ErrorMessage, Readable}
import com.typesafe.config.{Config, ConfigException}

import java.nio.file.{InvalidPathException, Path, Paths}

sealed abstract class ConfigError(
  val message: ErrorMessage,
  val path: NonEmptyList[String],
  inner: Option[Exception]
) extends Exception(message.message, inner.orNull) {
  def key: String = path.last
}
class MissingValue(path: NonEmptyList[String])
  extends ConfigError(
    ErrorMessage(s"Missing: ${path.toList.mkString(".")}."),
    path,
    None
  )
class InvalidValue(
  message: ErrorMessage,
  path: NonEmptyList[String],
  e: Option[Exception]
) extends ConfigError(message, path, e) {
  def this(path: NonEmptyList[String], e: Option[Exception]) =
    this(ErrorMessage(s"Failed to read '${path.last}' at ${path.toList.mkString(".")}."), path, e)
}

trait ConfigReadable[T] {
  def parseOpt(key: String, c: ConfigNode): Either[ConfigError, Option[T]]
  def read(key: String, c: ConfigNode): Either[ErrorMessage, T] =
    parse(key, c).left.map(_.message)
  def parse(key: String, c: ConfigNode): Either[ConfigError, T] =
    parseOpt(key, c).flatMap { opt =>
      opt.toRight(new MissingValue(append(key, to = c.position)))
    }
  def parseOrElse(key: String, c: ConfigNode, orElse: => T): Either[ConfigError, T] =
    parseOpt(key, c).map { opt =>
      opt.getOrElse(orElse)
    }
  def flatMap[U](f: T => ConfigReadable[U]): ConfigReadable[U] = {
    val parent = this
    (key: String, c: ConfigNode) =>
      parent
        .parseOpt(key, c)
        .flatMap(opt => opt.map(t => f(t).parseOpt(key, c)).getOrElse(Right(None)))
  }
  def emap[U](f: T => Either[ConfigError, U]): ConfigReadable[U] =
    flatMap { t => (key, _) =>
      f(t).map(u => Option(u))
    }
  def emapParsed[U](f: T => Either[ErrorMessage, U]): ConfigReadable[U] =
    flatMap { t => (key, c) =>
      f(t)
        .map(u => Option(u))
        .left
        .map(err => new InvalidValue(err, append(key, c.position), None))
    }
  def map[U](f: T => U): ConfigReadable[U] =
    emapParsed(t => Right(f(t)))
}

object ConfigReadable {
  implicit class ConfigOps(val c: Config) extends AnyVal {
    def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
      r.read(key, ConfigNode.root(c))
    def parse[T](key: String)(implicit r: ConfigReadable[T]): Either[ConfigError, T] =
      r.parse(key, ConfigNode.root(c))
    def opt[T](key: String)(implicit r: ConfigReadable[T]): Either[ConfigError, Option[T]] =
      r.parseOpt(key, ConfigNode.root(c))

    def bool(key: String, position: List[String]) = recovered(key, position, c.getBoolean)
    def int(key: String, position: List[String]) = recovered(key, position, c.getInt)
    def string(key: String, position: List[String]) = recovered(key, position, c.getString)
    def config(key: String, position: List[String]) = recovered(key, position, c.getConfig)
  }
  implicit val string: ConfigReadable[String] = (key: String, c: ConfigNode) =>
    c.conf.string(key, c.position)
  implicit val int: ConfigReadable[Int] = (key: String, c: ConfigNode) =>
    c.conf.int(key, c.position)
  implicit val bool: ConfigReadable[Boolean] = (key: String, c: ConfigNode) =>
    c.conf.bool(key, c.position)
  implicit val node: ConfigReadable[ConfigNode] = (key: String, c: ConfigNode) =>
    c.conf
      .config(key, c.position)
      .map(optConf => optConf.map(next => new ConfigNode(next, c.position ++ key.split('.'))))
  implicit val path: ConfigReadable[Path] = ConfigReadable.string.emapParsed { s =>
    try Right(Paths.get(s))
    catch {
      case ipe: InvalidPathException => Left(ErrorMessage(s"Invalid path: '$s'."))
    }
  }
  implicit def readable[T](implicit r: Readable[T]): ConfigReadable[T] =
    string.emapParsed(s => r.read(s))

  def recovered[T](
    key: String,
    position: List[String],
    unsafe: String => T
  ): Either[ConfigError, Option[T]] =
    try {
      Right(Option(unsafe(key)))
    } catch {
      case m: ConfigException.Missing =>
        Right(None)
      case e: Exception =>
        Left(new InvalidValue(append(key, position), Option(e)))
    }

  private def append(key: String, to: List[String]) =
    NonEmptyList.fromList(to ++ key.split('.')).getOrElse(NonEmptyList.of(key))
}
