package com.malliina.ws

import java.net.URI
import java.util
import javax.net.ssl.SSLContext

import com.malliina.http.FullUrl
import com.neovisionaries.ws.client._
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

object SocketClient {
  def factoryFor(sslContext: Option[SSLContext]): WebSocketFactory = {
    val factory = new WebSocketFactory
    sslContext foreach { ctx => factory setSSLContext ctx }
    factory
  }
}
abstract class SocketClient[T](val uri: FullUrl,
                               val factory: WebSocketFactory,
                               additionalHeaders: Map[String, String]) extends WebSocketBase[T] {
  def this(uri: FullUrl, sslContext: Option[SSLContext], headers: Map[String, String]) =
    this(uri, SocketClient.factoryFor(sslContext), headers)

  protected val connectTimeout = 10.seconds
  protected val connectPromise = Promise[Unit]()
  protected val subject: Subject[T] = Subject[T]().toSerialized

  val socket = factory.createSocket(uri.url, connectTimeout.toMillis.toInt)
  additionalHeaders foreach {
    case (key, value) => socket.addHeader(key, value)
  }
  socket.addListener(new WebSocketAdapter {
    override def onConnected(websocket: WebSocket, headers: util.Map[String, util.List[String]]): Unit = {
      connectPromise.trySuccess(())
      SocketClient.this.onConnect(websocket.getURI)
    }

    override def onTextMessage(websocket: WebSocket, text: String): Unit = {
      SocketClient.this.onRawMessage(text)
    }

    override def onDisconnected(websocket: WebSocket,
                                serverCloseFrame: WebSocketFrame,
                                clientCloseFrame: WebSocketFrame,
                                closedByServer: Boolean): Unit = {
      val uri = websocket.getURI
      val suffix = if (closedByServer) " by the server" else ""
      connectPromise tryFailure new NotConnectedException(s"The websocket to $uri was closed$suffix.")
      SocketClient.this.onClose()
      subject.onCompleted()
    }

    override def onError(websocket: WebSocket, cause: WebSocketException): Unit = {
      //      log.error(s"Websocket error for ${websocket.getURI.toString}", cause)
      connectPromise tryFailure cause
      SocketClient.this.onError(cause)
      subject.onError(cause)
    }
  })

  def onConnect(uri: URI): Unit = ()

  def onMessage(json: T) = ()

  override def onClose(): Unit = ()

  override def onError(t: Exception): Unit = ()

  protected def parse(raw: String): Option[T]

  protected def stringify(message: T): String

  def messages: Observable[T] = subject

  /** Only call this method once per instance.
    *
    * Impl: On subsequent calls, the returned future will always be completed regardless of connection result
    *
    * @return a [[Future]] that completes successfully when the connection has been established or fails otherwise
    */
  override def connect(): Future[Unit] = {
    Try(socket.connectAsynchronously()) match {
      case Success(_) =>
        connectPromise.future
      case Failure(t) =>
        connectPromise tryFailure t
        connectPromise.future
    }
  }

  def onRawMessage(raw: String) = parse(raw).foreach { msg =>
    onMessage(msg)
    subject onNext msg
  }

  /**
    * @param json payload
    * @return
    */
  override def send(json: T): Try[Unit] = Try(socket.sendText(stringify(json)))

  def isConnected = socket.isOpen

  def close(): Unit = socket.disconnect()
}
