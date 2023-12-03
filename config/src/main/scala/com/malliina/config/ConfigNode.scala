package com.malliina.config

import com.typesafe.config.{Config, ConfigFactory}

import java.nio.file.Path

class ConfigNode(val conf: Config, val position: List[String]) {
  def parse[T](key: String)(implicit r: ConfigReadable[T]): Either[ConfigError, T] =
    r.parse(key, this)
  def parseOrElse[T](key: String, orElse: => T)(implicit
    r: ConfigReadable[T]
  ): Either[ConfigError, T] =
    r.parseOrElse(key, this, orElse)
  def opt[T](key: String)(implicit r: ConfigReadable[T]): Either[ConfigError, Option[T]] =
    r.parseOpt(key, this)
}

object ConfigNode {
  def root(config: Config = ConfigFactory.load()): ConfigNode =
    new ConfigNode(config.resolve(), Nil)
  def load(resourceBaseName: String): ConfigNode =
    root(ConfigFactory.load(resourceBaseName))
  def default(file: Path): ConfigNode =
    root(ConfigFactory.parseFile(file.toFile).withFallback(ConfigFactory.load()))
  def parseFile(file: Path): ConfigNode =
    root(ConfigFactory.parseFile(file.toFile))
}
