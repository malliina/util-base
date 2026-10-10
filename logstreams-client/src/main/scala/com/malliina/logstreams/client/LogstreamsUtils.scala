package com.malliina.logstreams.client

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.HttpClient
import com.malliina.logback.LogbackUtils
import com.malliina.values.ErrorMessage

import scala.concurrent.duration.{DurationInt, FiniteDuration}

case class LogstreamsConf(
  enabled: Boolean,
  user: String,
  pass: String,
  userAgent: String,
  emitInterval: FiniteDuration
)

object LogstreamsConf:
  val DefaultEmitInterval: FiniteDuration = 1.second

  def isEnabled = sys.env.get("LOGSTREAMS_ENABLED").contains("true")

  def read(
    defaultUser: String,
    userAgent: String,
    emitInterval: FiniteDuration = DefaultEmitInterval
  ): Either[ErrorMessage, LogstreamsConf] =
    for pass <- LogstreamsUtils.env("LOGSTREAMS_PASS")
    yield LogstreamsConf(
      isEnabled,
      sys.env.getOrElse("LOGSTREAMS_USER", defaultUser),
      pass,
      userAgent,
      emitInterval
    )

object LogstreamsUtils:
  def resource[F[_]: Async](
    conf: LogstreamsConf,
    d: Dispatcher[F],
    http: HttpClient[F]
  ): Resource[F, Unit] =
    Resource.make(install(conf, d, http))(_ => Sync[F].delay(LogbackUtils.loggerContext.stop()))

  def installIfEnabled[F[_]: Async](
    defaultUser: String,
    userAgent: String,
    emitInterval: FiniteDuration,
    d: Dispatcher[F],
    http: HttpClient[F]
  ): F[Boolean] =
    if LogstreamsConf.isEnabled then
      LogstreamsConf
        .read(defaultUser, userAgent, emitInterval)
        .fold(
          err => Async[F].raiseError(IllegalArgumentException(err.message)),
          ok => install(ok, d, http)
        )
        .map(_ => true)
    else Async[F].pure(false)

  def install[F[_]: Async](
    conf: LogstreamsConf,
    d: Dispatcher[F],
    http: HttpClient[F]
  ): F[Unit] =
    val lc = LogbackUtils.loggerContext
    FS2Appender
      .default(
        d,
        http,
        conf.emitInterval,
        Map(HttpUtil.UserAgent -> conf.userAgent)
      )
      .flatMap: appender =>
        Async[F].delay:
          appender.setContext(lc)
          appender.setName("LOGSTREAMS")
          appender.setEndpoint("wss://logs.malliina.com/ws/sources")
          appender.setUsername(conf.user)
          appender.setPassword(conf.pass)
          appender.setEnabled(conf.enabled)
          LogbackUtils.installAppender(appender)

  def env(key: String): Either[ErrorMessage, String] =
    sys.env.get(key).toRight(ErrorMessage(s"No $key environment variable set."))
