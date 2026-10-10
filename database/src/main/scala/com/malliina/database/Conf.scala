package com.malliina.database

import com.malliina.config.ConfigReadable
import com.malliina.http.FullUrl
import com.malliina.values.{Password, Readable}

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class Conf(
  url: FullUrl,
  user: String,
  pass: Password,
  driver: String,
  maxPoolSize: Int,
  maxLifetime: FiniteDuration,
  keepaliveTime: FiniteDuration,
  idleTimeout: FiniteDuration,
  autoMigrate: Boolean,
  schemaTable: String = Conf.DefaultSchemaTable
)

object Conf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultSchemaTable = "flyway_schema_history"
  val DefaultMaxLifetime: FiniteDuration = 30.minutes
  val DefaultKeepaliveTime: FiniteDuration = 2.minutes
  val DefaultIdleTimeout: FiniteDuration = 10.minutes

  given Readable[FiniteDuration] = Readable.int.map(_.seconds)
  given Readable[FullUrl] = FullUrl.readable
  given ConfigReadable[Conf] = ConfigReadable.node.emap: c =>
    for
      url <- c.parse[FullUrl]("url")
      user <- c.parse[String]("user")
      pass <- c.parse[Password]("pass")
      driver <- c.parse[String]("driver")
      maxPoolSize <- c.parse[Int]("maxPoolSize")
      maxLifetime <- c.opt[FiniteDuration]("maxLifetime")
      keepaliveTime <- c.opt[FiniteDuration]("keepaliveTime")
      idleTimeout <- c.opt[FiniteDuration]("idleTimeout")
      autoMigrate <- c.parse[Boolean]("autoMigrate")
      schemaTable <- c.opt[String]("schemaTable")
    yield Conf(
      url,
      user,
      pass,
      driver,
      maxPoolSize,
      maxLifetime.getOrElse(DefaultMaxLifetime),
      keepaliveTime.getOrElse(DefaultKeepaliveTime),
      idleTimeout.getOrElse(DefaultIdleTimeout),
      autoMigrate,
      schemaTable.getOrElse(DefaultSchemaTable)
    )
