package com.malliina.http

import java.io.IOException
import java.nio.file.Path
import java.util
import java.util.concurrent.Executors

import javax.net.ssl.{SSLSocketFactory, X509TrustManager}
import okhttp3._

import scala.concurrent.{ExecutionContext, Future, Promise}

object OkClient {
  val jsonMediaType: MediaType = MediaType.parse("application/json")
  val plainText = MediaType.parse("text/plain")

  def default: OkClient = apply(okHttpClient)

  def ssl(ssf: SSLSocketFactory, tm: X509TrustManager): OkClient =
    apply(sslClient(ssf, tm))

  def apply(client: OkHttpClient = okHttpClient): OkClient =
    new OkClient(client, defaultExecutionContext())

  def okHttpClient: OkHttpClient =
    new OkHttpClient.Builder()
      .protocols(util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
      .build()

  def sslClient(ssf: SSLSocketFactory, tm: X509TrustManager): OkHttpClient =
    new OkHttpClient.Builder()
      .sslSocketFactory(ssf, tm)
      .protocols(util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
      .build()

  def defaultExecutionContext() =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  case class MultiPartFile(partName: String, fileName: String, mediaType: MediaType, file: Path)

  object MultiPartFile {
    def apply(mediaType: MediaType, file: Path): MultiPartFile =
      MultiPartFile("file", file.getFileName.toString, mediaType, file)
  }
}

class OkClient(val client: OkHttpClient, ec: ExecutionContext)
  extends HttpClient[Future]
  with OkHttpBackend {
  implicit val exec: ExecutionContext = ec

  def execute(request: Request): Future[OkHttpResponse] =
    raw(request).map(OkHttpResponse.apply)

  /** Provides the response but closes the response body after `consume` completes.
    *
    * @param request
    *   request
    * @param consume
    *   code to consume the response
    * @tparam T
    *   type of result
    * @return
    */
  def streamed[T](request: Request)(consume: Response => Future[T]): Future[T] = {
    raw(request).flatMap { r =>
      val work = consume(r)
      work.onComplete(_ => r.close())
      work
    }
  }

  /** Remember to close the response body if calling this method. If you don't need to stream the
    * response, call `execute` instead.
    *
    * @param request
    *   request to execute
    * @return
    *   the response
    */
  def raw(request: Request): Future[Response] = {
    val (future, callback) = RawCallback.paired()
    client.newCall(request).enqueue(callback)
    future
  }

  override def flatMap[T, U](t: Future[T])(f: T => Future[U]): Future[U] = t.flatMap(f)
  override def success[T](t: T): Future[T] = Future.successful(t)
  override def fail[T](e: Exception): Future[T] = Future.failed(e)
}

class RawCallback(p: Promise[Response]) extends Callback {
  override def onFailure(call: Call, e: IOException): Unit =
    p.tryFailure(e)

  override def onResponse(call: Call, response: Response): Unit =
    p.trySuccess(response)
}

object RawCallback {
  def paired() = {
    val p = Promise[Response]()
    val callback = new RawCallback(p)
    (p.future, callback)
  }
}
