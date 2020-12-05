package com.malliina.http.io

import java.io.IOException

import cats.effect.IO
import com.malliina.http.io.HttpClientIO.CallOps
import com.malliina.http.{HttpClient, OkClient, OkHttpBackend, OkHttpResponse}
import okhttp3._

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

class HttpClientIO(val client: OkHttpClient) extends HttpClient[IO] with OkHttpBackend {
  override def streamed[T](request: Request)(consume: Response => IO[T]): IO[T] =
    raw(request).bracket(consume)(r => IO(r.close()))
  override def execute(request: Request): IO[OkHttpResponse] =
    raw(request).map(OkHttpResponse.apply)
  override def raw(request: Request): IO[Response] =
    client.newCall(request).io

  override def flatMap[T, U](t: IO[T])(f: T => IO[U]): IO[U] = t.flatMap(f)
  override def success[T](t: T): IO[T] = IO.pure(t)
  override def fail[T](e: Exception): IO[T] = IO.raiseError(e)
}
