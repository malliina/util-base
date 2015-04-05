package com.mle.ws

import java.net.URI
import javax.net.ssl.SSLContext

import com.mle.concurrent.{ExecutionContexts, Futures}
import org.java_websocket.client.{DefaultSSLWebSocketClientFactory, WebSocketClient}
import org.java_websocket.drafts.Draft_10
import org.java_websocket.handshake.ServerHandshake
import rx.lang.scala.{Observable, Subject}

import scala.collection.JavaConversions._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
abstract class SocketClient[T](uri: String,
                               sslContext: Option[SSLContext],
                               additionalHeaders: (String, String)*) extends WebSocketBase[T] {
  protected val connectTimeout = 10.seconds
  protected val connectPromise = Promise[Unit]()
  protected val subject = Subject[T]()
  val headers: java.util.Map[String, String] = Map.empty[String, String] ++ additionalHeaders.toMap

  protected def parse(raw: String): Option[T]

  protected def stringify(message: T): String

  def messages: Observable[T] = subject

  val client = new WebSocketClient(URI create uri, new Draft_10, headers, 0) {
    def onOpen(handshakedata: ServerHandshake) {
      //      log info s"Opened websocket to: $uri"
      connectPromise.trySuccess(())
      //      subject onNext SocketConnected
    }

    def onMessage(message: String): Unit = {
      SocketClient.this.onRawMessage(message)
    }

    /**
     *
     * @param code 1000 if the client disconnects normally, 1006 if the server dies abnormally
     * @param reason
     * @param remote
     * @see http://tools.ietf.org/html/rfc6455#section-7.4.1
     */
    def onClose(code: Int, reason: String, remote: Boolean) {
      //      log info s"Closed websocket to: $uri, code: $code, reason: $reason, remote: $remote"
      connectPromise tryFailure new NotConnectedException(s"The websocket was closed. Code: $code, reason: $reason.")
      SocketClient.this.onClose()
      //      eventsSubject onNext Disconnected
      // not sure if I should do this; the connect() docs says it "maintains" a connection?
      subject.onCompleted()
    }

    /**
     * Exceptions thrown in this handler like in onMessage end up here.
     *
     * If the connection attempt fails, this is called with a [[java.net.ConnectException]].
     */
    def onError(ex: Exception) {
      //      log.warn("WebSocket error", ex)
      connectPromise tryFailure ex
      SocketClient.this.onError(ex)
      subject onError ex
    }
  }
  if (uri startsWith "wss") {
    sslContext.foreach(ctx => {
      val factory = new DefaultSSLWebSocketClientFactory(ctx)
      client setWebSocketFactory factory
    })
  }

  /**
   * Only call this method once per instance.
   *
   * Impl: On subsequent calls, the returned future will always be completed regardless of connection result
   *
   * @return a [[Future]] that completes successfully when the connection has been established or fails otherwise
   */
  def connect(): Future[Unit] = {
    Try(client.connect()) match {
      case Success(()) =>
        Futures.within(connectTimeout)(connectPromise.future)(ExecutionContexts.cached)
      case Failure(t) =>
        connectPromise tryFailure t
        connectPromise.future
    }
  }

  def onRawMessage(raw: String) = parse(raw).foreach(msg => {
    onMessage(msg)
    subject onNext msg
  })

  def onMessage(json: T) = ()

  /**
   * Might at least fail with a [[java.nio.channels.NotYetConnectedException]] or [[java.io.IOException]].
   *
   * @param json payload
   * @return
   */
  override def send(json: T): Try[Unit] = Try {
    client send stringify(json)
  }

  def isConnected = client.getConnection.isOpen

  def close(): Unit = client.close()

  override def onClose(): Unit = ()

  override def onError(t: Exception): Unit = ()
}
