package com.malliina.http

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util
import java.util.concurrent.Executors

import com.malliina.http.OkClient.OkResponse
import javax.net.ssl.{SSLSocketFactory, X509TrustManager}
import okhttp3._
import play.api.libs.json.{JsValue, Json, Reads}

import scala.concurrent.{ExecutionContext, Future, Promise}

object OkClient {
  val jsonMediaType: MediaType = MediaType.parse("application/json")

  type OkResponse[T] = Future[Either[ResponseError, T]]

  def default: OkClient = apply(okHttpClient)

  def ssl(ssf: SSLSocketFactory, tm: X509TrustManager): OkClient =
    apply(sslClient(ssf, tm))

  def apply(client: OkHttpClient): OkClient = new OkClient(client, defaultExecutionContext())

  def okHttpClient: OkHttpClient = new OkHttpClient.Builder()
    .protocols(util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
    .build()

  def sslClient(ssf: SSLSocketFactory, tm: X509TrustManager): OkHttpClient =
    new OkHttpClient.Builder()
      .sslSocketFactory(ssf, tm)
      .protocols(util.Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
      .build()

  def defaultExecutionContext() =
    ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
}

class OkClient(val client: OkHttpClient, ec: ExecutionContext) {
  implicit val exec = ec

  def getJson[T: Reads](url: FullUrl, headers: Map[String, String] = Map.empty): OkResponse[T] =
    get(url, headers).map(r => parse[T](r, url))

  def get(url: FullUrl, headers: Map[String, String] = Map.empty): Future[OkHttpResponse] = {
    val req = headers.foldLeft(new Request.Builder().url(url.url)) {
      case (r, (key, value)) => r.addHeader(key, value)
    }
    execute(req.get().build())
  }

  def postJsonAs[T: Reads](url: FullUrl,
                           json: JsValue,
                           headers: Map[String, String] = Map.empty): OkResponse[T] =
    postJson(url, json, headers).map(r => parse[T](r, url))

  def postJson(url: FullUrl,
               json: JsValue,
               headers: Map[String, String] = Map.empty): Future[OkHttpResponse] =
    post(url, RequestBody.create(OkClient.jsonMediaType, Json.stringify(json)), Map.empty)

  def postFile(url: FullUrl,
               mediaType: MediaType,
               file: Path,
               headers: Map[String, String] = Map.empty): Future[OkHttpResponse] =
    post(url, RequestBody.create(mediaType, file.toFile), headers)

  def postFormAs[T: Reads](url: FullUrl,
                           form: Map[String, String],
                           headers: Map[String, String] = Map.empty): OkResponse[T] =
    postForm(url, form, headers).map(r => parse[T](r, url))

  def postForm(url: FullUrl,
               form: Map[String, String],
               headers: Map[String, String] = Map.empty): Future[OkHttpResponse] = {
    val bodyBuilder = new FormBody.Builder(StandardCharsets.UTF_8)
    form foreach { case (k, v) =>
      bodyBuilder.add(k, v)
    }
    post(url, bodyBuilder.build(), headers)
  }

  def post(url: FullUrl, body: RequestBody, headers: Map[String, String]): Future[OkHttpResponse] = {
    val builder = new Request.Builder().url(url.url).post(body)
    headers.foreach { case (k, v) =>
      builder.header(k, v)
    }
    execute(builder.build())
  }

  def parse[T: Reads](response: OkHttpResponse, url: FullUrl): Either[ResponseError, T] =
    if (response.isSuccess) response.parse[T].left.map(err => JsonError(err, response, url))
    else Left(StatusError(response, url))

  def execute(request: Request): Future[OkHttpResponse] = {
    val (future, callback) = PromisingCallback.paired()
    client.newCall(request).enqueue(callback)
    future
  }
}

class PromisingCallback(p: Promise[OkHttpResponse]) extends Callback {
  override def onFailure(call: Call, e: IOException): Unit =
    p.tryFailure(e)

  override def onResponse(call: Call, response: Response): Unit =
    p.trySuccess(new OkHttpResponse(response))
}

object PromisingCallback {
  def paired() = {
    val p = Promise[OkHttpResponse]()
    val callback = new PromisingCallback(p)
    (p.future, callback)
  }
}
