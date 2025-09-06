package com.malliina.http

sealed trait SocketEvent

object SocketEvent {
  case object Idle extends SocketEvent
  case class Open(url: FullUrl) extends SocketEvent
  case class Closing(url: FullUrl, code: Int, reason: String) extends SocketEvent
  case class Closed(url: FullUrl, code: Int, reason: String) extends SocketEvent {
    def exception: WebSocketException = new WebSocketException(url, None)
  }
  case class Failure(url: FullUrl, t: Option[Throwable]) extends SocketEvent {
    def exception: WebSocketException = new WebSocketException(url, t)
  }
  case class TextMessage(url: FullUrl, message: String) extends SocketEvent
  case class BytesMessage(url: FullUrl, bytes: Array[Byte]) extends SocketEvent
}

class WebSocketException(url: FullUrl, t: Option[Throwable])
  extends Exception(s"Socket to $url failed or closed.", t.orNull)
