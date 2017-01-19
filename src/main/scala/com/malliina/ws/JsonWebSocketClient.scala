package com.malliina.ws

import javax.net.ssl.SSLContext

import play.api.libs.json.{JsValue, Json, Writes}

import scala.util.Try

class JsonWebSocketClient(uri: String,
                          sslContext: Option[SSLContext],
                          additionalHeaders: (String, String)*)
  extends SocketClient[JsValue](uri, sslContext, additionalHeaders: _*) {

  def sendMessage[T: Writes](message: T): Try[Unit] = send(Json toJson message)

  override protected def parse(raw: String): Option[JsValue] = Try(Json parse raw).toOption

  override protected def stringify(message: JsValue): String = Json stringify message
}

object JsonWebSocketClient {

  trait SocketEvent

  case class SocketMessage(json: JsValue) extends SocketEvent

  case object Connecting extends SocketEvent

  case object Connected extends SocketEvent

}
