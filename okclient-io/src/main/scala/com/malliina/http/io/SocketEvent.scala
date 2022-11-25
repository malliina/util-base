package com.malliina.http.io

import okhttp3.{Response, WebSocket}
import okio.ByteString

sealed trait SocketEvent

object SocketEvent {
  case object Idle extends SocketEvent
  case class Open(socket: WebSocket, response: Response) extends SocketEvent
  case class Closing(socket: WebSocket, code: Int, reason: String) extends SocketEvent
  case class Closed(socket: WebSocket, code: Int, reason: String) extends SocketEvent {
    def exception: WebSocketException = new WebSocketException(socket, None, None)
  }
  case class Failure(socket: WebSocket, t: Option[Throwable], response: Option[Response])
    extends SocketEvent {
    def exception: WebSocketException = new WebSocketException(socket, t, response)
  }
  case class TextMessage(socket: WebSocket, message: String) extends SocketEvent
  case class BytesMessage(socket: WebSocket, bytes: ByteString) extends SocketEvent
}

class WebSocketException(socket: WebSocket, t: Option[Throwable], response: Option[Response])
  extends Exception(s"Socket to ${socket.request().url()} failed or closed.", t.orNull)
