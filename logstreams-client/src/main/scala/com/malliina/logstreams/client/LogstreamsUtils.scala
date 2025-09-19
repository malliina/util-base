package com.malliina.logstreams.client

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Resource, Sync}
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.HttpClient
import com.malliina.logback.LogbackUtils
import com.malliina.values.ErrorMessage

case class LogstreamsConf(enabled: Boolean, user: String, pass: String, userAgent: String)

object LogstreamsConf:
  def isEnabled = sys.env.get("LOGSTREAMS_ENABLED").contains("true")

  def read(defaultUser: String, userAgent: String): Either[ErrorMessage, LogstreamsConf] =
    for pass <- LogstreamsUtils.env("LOGSTREAMS_PASS")
    yield LogstreamsConf(
      isEnabled,
      sys.env.getOrElse("LOGSTREAMS_USER", defaultUser),
      pass,
      userAgent
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
    d: Dispatcher[F],
    http: HttpClient[F]
  ) =
    if LogstreamsConf.isEnabled then
      LogstreamsConf
        .read(defaultUser, userAgent)
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
