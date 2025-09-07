package com.malliina.http.io

import cats.effect.Sync
import cats.effect.std.Dispatcher
import com.malliina.http.SocketEvent.{BytesMessage, Closed, Closing, Failure, Open, TextMessage}
import com.malliina.http.io.OkListener.log
import com.malliina.http.{FullUrl, SocketEvent}
import com.malliina.util.AppLogger
import fs2.concurrent.Topic
import okhttp3.{Response, WebSocket, WebSocketListener}
import okio.ByteString

import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.{ExecutionContext, Future}

object OkListener {
  private val log = AppLogger(getClass)
}

class OkListener[F[_]: Sync](
  url: FullUrl,
  topic: Topic[F, SocketEvent],
  interrupted: AtomicBoolean,
  d: Dispatcher[F]
) {
  private def publish(e: SocketEvent): Unit = {
    val writeLog: (String, Throwable) => Unit =
      if (interrupted.get()) log.debug else log.warn
    implicit val parasitic: ExecutionContext = new ExecutionContext {
      def execute(runnable: Runnable): Unit = runnable.run()

      def reportFailure(t: Throwable): Unit = writeLog(s"Failed to execute.", t)
    }
    Future(d.unsafeToFuture(topic.publish1(e))).flatten.onComplete {
      case util.Failure(exception) =>
        writeLog(s"Failed to publish message $e to '$url'.", exception)
      case util.Success(value) =>
        value match {
          case Left(value)  => log.warn(s"Failed to publish message $e to '$url', topic closed.")
          case Right(value) => ()
        }
    }
  }

  val listener: WebSocketListener = new WebSocketListener {
    override def onClosed(webSocket: WebSocket, code: Int, reason: String): Unit = {
      log.info(s"Closed socket to '$url'.")
      publish(Closed(url, code, reason))
    }

    override def onClosing(webSocket: WebSocket, code: Int, reason: String): Unit = {
      log.info(s"Closing socket to '$url'.")
      publish(Closing(url, code, reason))
    }

    override def onFailure(webSocket: WebSocket, t: Throwable, response: Response): Unit = {
      if (!interrupted.get())
        log.warn(s"Socket to '$url' failed.", t)
      publish(Failure(url, Option(t)))
    }

    override def onMessage(webSocket: WebSocket, text: String): Unit = {
      log.debug(s"Received text '$text'.")
      publish(TextMessage(url, text))
    }

    override def onMessage(webSocket: WebSocket, bytes: ByteString): Unit = {
      log.debug(s"Received bytes $bytes")
      publish(BytesMessage(url, bytes.toByteArray))
    }

    override def onOpen(webSocket: WebSocket, response: Response): Unit = {
      log.info(s"Opened socket to '$url'.")
      publish(Open(url))
    }
  }
}
