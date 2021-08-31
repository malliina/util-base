package com.malliina.http.io

import cats.MonadError
import cats.effect.{Concurrent, ContextShift, IO, Timer}
import com.malliina.http.io.HttpClientIO.CallOps
import com.malliina.http.{FullUrl, HttpClient, OkClient, OkHttpBackend, OkHttpResponse}
import okhttp3._

import java.io.IOException

object HttpClientIO {
  def apply(http: OkHttpClient = OkClient.okHttpClient): HttpClientIO = new HttpClientIO(http)

  implicit class CallOps(val call: Call) extends AnyVal {
    def io: IO[Response] = run(call)
  }

  def run(call: Call): IO[Response] = IO.async { cb =>
    call.enqueue(new Callback {
      override def onResponse(call: Call, response: Response): Unit =
        cb(Right(response))
      override def onFailure(call: Call, e: IOException): Unit =
        cb(Left(e))
    })
  }
}

class HttpClientIO(val client: OkHttpClient) extends HttpClientF[IO] with OkHttpBackend {
  override def streamed[T](request: Request)(consume: Response => IO[T]): IO[T] =
    raw(request).bracket(consume)(r => IO(r.close()))
  override def raw(request: Request): IO[Response] =
    client.newCall(request).io
  def socket(
    url: FullUrl,
    headers: Map[String, String],
    cs: ContextShift[IO],
    timer: Timer[IO]
  ): IO[WebSocketIO] = WebSocketIO(url, headers, client)(cs, timer)
}

abstract class HttpClientF[F[_]]()(implicit F: MonadError[F, Throwable]) extends HttpClient[F] {
  override def execute(request: Request): F[OkHttpResponse] =
    F.map(raw(request))(OkHttpResponse.apply)
  override def flatMap[T, U](t: F[T])(f: T => F[U]): F[U] = F.flatMap(t)(f)
  override def success[T](t: T): F[T] = F.pure(t)
  override def fail[T](e: Exception): F[T] = F.raiseError(e)
}
