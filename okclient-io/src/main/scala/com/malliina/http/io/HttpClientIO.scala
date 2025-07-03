package com.malliina.http.io

import cats.MonadError
import cats.effect.implicits.monadCancelOps_
import cats.effect.{Async, IO, Sync}
import cats.effect.kernel.Resource
import com.malliina.http.io.HttpClientIO.CallOps
import com.malliina.http.{FullUrl, HttpClient, OkClient, OkHttpBackend, OkHttpResponse}
import okhttp3._

import java.io.IOException

object HttpClientIO {
  def resource[F[_]: Async]: Resource[F, HttpClientF2[F]] =
    configure(identity)

  def configure[F[_]: Async](
    customize: OkHttpClient.Builder => OkHttpClient.Builder = identity
  ): Resource[F, HttpClientF2[F]] =
    Resource.make(Async[F].delay(new HttpClientF2(OkClient.newClient(customize))))(c =>
      Async[F].delay(c.close())
    )

  def apply(http: OkHttpClient = OkClient.okHttpClient): HttpClientIO = new HttpClientIO(http)

  implicit class CallOps(val call: Call) extends AnyVal {
    def io[F[_]: Async]: F[Response] = run(call)
  }

  def run[F[_]: Async](call: Call): F[Response] = Async[F].async_ { cb =>
    call.enqueue(new Callback {
      override def onResponse(call: Call, response: Response): Unit =
        cb(Right(response))

      override def onFailure(call: Call, e: IOException): Unit =
        cb(Left(e))
    })
  }
}

class HttpClientIO(client: OkHttpClient) extends HttpClientF2[IO](client)

class HttpClientF2[F[_]: Async](val client: OkHttpClient = OkClient.okHttpClient)
  extends HttpClientF[F]
  with OkHttpBackend {
  override def streamed[T](request: Request)(consume: Response => F[T]): F[T] =
    raw(request).bracket(consume)(r => Sync[F].delay(r.close()))
  override def raw(request: Request): F[Response] =
    client.newCall(request).io
  def socket(
    url: FullUrl,
    headers: Map[String, String]
  ): Resource[F, WebSocketF[F]] = WebSocketF.build(url, headers, client)
}

abstract class HttpClientF[F[_]: Sync]()(implicit F: MonadError[F, Throwable])
  extends HttpClient[F] {
  override def execute(request: Request): F[OkHttpResponse] =
    F.map(raw(request))(OkHttpResponse.apply)
  override def flatMap[T, U](t: F[T])(f: T => F[U]): F[U] = F.flatMap(t)(f)
  override def success[T](t: T): F[T] = Sync[F].blocking(t)
  override def fail[T](e: Exception): F[T] = F.raiseError(e)
}
