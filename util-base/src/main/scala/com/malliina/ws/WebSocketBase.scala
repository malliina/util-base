package com.malliina.ws

import io.circe.Json
import scala.concurrent.Future
import scala.util.Try

trait JsonWebSocket extends WebSocketBase[Json]

/** @tparam T
  *   type of message sent over the websocket connection
  */
trait WebSocketBase[T] {

  /** @return
    *   a future that completes when the connection has successfully been established
    */
  def connect(): Future[Unit]
  def send(json: T): Try[Unit]
  def onMessage(json: T): Unit
  def onClose(): Unit
  def onError(t: Exception): Unit
  def close(): Unit
}

class NotConnectedException(msg: String) extends Exception(msg)
