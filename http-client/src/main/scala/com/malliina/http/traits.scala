package com.malliina.http

import fs2.concurrent.Topic
import io.circe.Encoder
import io.circe.syntax.EncoderOps
import org.typelevel.ci.CIString

trait SocketBuilder[F[_], S <: WebSocketOps[F]] {
  def url: FullUrl
  def connect(sink: Topic[F, SocketEvent], headers: Map[CIString, String]): F[S]
}

trait WebSocketOps[F[_]] {
  def send[T: Encoder](message: T): F[Boolean] = sendMessage(message.asJson.noSpaces)
  def sendMessage(s: String): F[Boolean]
  def closeNow: F[Unit]
}
