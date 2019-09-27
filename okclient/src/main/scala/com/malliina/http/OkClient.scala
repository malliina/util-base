package com.malliina.http

import java.io.{Closeable, IOException}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}
import java.util
import java.util.concurrent.Executors

import com.malliina.http.OkClient.MultiPartFile
import com.malliina.storage.{StorageLong, StorageSize}
import javax.net.ssl.{SSLSocketFactory, X509TrustManager}
import okhttp3._
import play.api.libs.json.{JsValue, Json, Reads, Writes}

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

class OkClient(val client: OkHttpClient, ec: ExecutionContext) extends Closeable {
  implicit val exec = ec

  def getAs[T: Reads](url: FullUrl, headers: Map[String, String] = Map.empty): Future[T] =
    get(url, headers).flatMap(r => parse[T](r, url))

  def get(url: FullUrl, headers: Map[String, String] = Map.empty): Future[OkHttpResponse] = {
    val req = requestFor(url, headers)
    execute(req.get().build())
  }

  /** Downloads `url` to `to`, returning the number of bytes written to `to`.
    *
    * @param url     url to download
    * @param to      destination, a file
    * @param headers http headers
    * @return bytes written
    */
  def download(url: FullUrl,
               to: Path,
               headers: Map[String, String] = Map.empty): Future[Either[StatusError, StorageSize]] =
    streamed(requestFor(url, headers).get().build()) { response =>
      Future.successful {
        if (response.isSuccessful)
          Right(
            Files.copy(response.body().byteStream(), to, StandardCopyOption.REPLACE_EXISTING).bytes)
        else Left(StatusError(OkHttpResponse(response), url))
      }
    }

  def postAs[W: Writes, T: Reads](url: FullUrl,
                                  json: W,
                                  headers: Map[String, String] = Map.empty): Future[T] =
    postJson(url, Json.toJson(json), headers).flatMap(r => parse[T](r, url))

  def postJsonAs[T: Reads](url: FullUrl,
                           json: JsValue,
                           headers: Map[String, String] = Map.empty): Future[T] =
    postJson(url, json, headers).flatMap(r => parse[T](r, url))

  def postJson(url: FullUrl,
               json: JsValue,
               headers: Map[String, String] = Map.empty): Future[OkHttpResponse] =
    post(url, RequestBody.create(Json.stringify(json), OkClient.jsonMediaType), headers)

  def postFile(url: FullUrl,
               mediaType: MediaType,
               file: Path,
               headers: Map[String, String] = Map.empty): Future[OkHttpResponse] =
    post(url, RequestBody.create(file.toFile, mediaType), headers)

  def postFormAs[T: Reads](url: FullUrl,
                           form: Map[String, String],
                           headers: Map[String, String] = Map.empty): Future[T] =
    postForm(url, form, headers).flatMap(r => parse[T](r, url))

  def postForm(url: FullUrl,
               form: Map[String, String],
               headers: Map[String, String] = Map.empty): Future[OkHttpResponse] = {
    val bodyBuilder = new FormBody.Builder(StandardCharsets.UTF_8)
    form foreach {
      case (k, v) =>
        bodyBuilder.add(k, v)
    }
    post(url, bodyBuilder.build(), headers)
  }

  def post(url: FullUrl,
           body: RequestBody,
           headers: Map[String, String]): Future[OkHttpResponse] = {
    val builder = requestFor(url, headers).post(body)
    execute(builder.build())
  }

  def multiPart(url: FullUrl,
                headers: Map[String, String] = Map.empty,
                parts: Map[String, String] = Map.empty,
                files: Seq[MultiPartFile] = Nil): Future[OkHttpResponse] = {
    val bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM)
    parts.foreach {
      case (k, v) =>
        bodyBuilder.addFormDataPart(k, v)
    }
    files.foreach { filePart =>
      bodyBuilder.addFormDataPart(filePart.partName,
                                  filePart.fileName,
                                  RequestBody.create(filePart.file.toFile, filePart.mediaType))
    }
    post(url, bodyBuilder.build(), headers)
  }

  /** Parses the response as a T.
    *
    * The returned Future fails with a ResponseError if parsing fails.
    *
    * @param response HTTP response
    * @param url the request URL
    * @tparam T type to parse
    * @return a parsed response
    */
  def parse[T: Reads](response: OkHttpResponse, url: FullUrl): Future[T] =
    if (response.isSuccess) {
      response
        .parse[T]
        .fold(err => Future.failed(JsonError(err, response, url).toException),
              ok => Future.successful(ok))
    } else {
      Future.failed(StatusError(response, url).toException)
    }

  def execute(request: Request): Future[OkHttpResponse] =
    raw(request).map(OkHttpResponse.apply)

  /** Provides the response but closes the response body after `consume` completes.
    *
    * @param request request
    * @param consume code to consume the response
    * @tparam T type of result
    * @return
    */
  def streamed[T](request: Request)(consume: Response => Future[T]): Future[T] = {
    raw(request).flatMap { r =>
      val work = consume(r)
      work.onComplete(_ => r.close())
      work
    }
  }

  /** Remember to close the response body if calling this method. If you don't need to stream the response,
    * call `execute` instead.
    *
    * @param request request to execute
    * @return the response
    */
  def raw(request: Request): Future[Response] = {
    val (future, callback) = RawCallback.paired()
    client.newCall(request).enqueue(callback)
    future
  }

  override def close(): Unit = {
    client.dispatcher().executorService().shutdown()
    client.connectionPool().evictAll()
    Option(client.cache()).foreach(_.close())
  }

  def requestFor(url: FullUrl, headers: Map[String, String]) =
    headers.foldLeft(new Request.Builder().url(url.url)) {
      case (r, (key, value)) => r.addHeader(key, value)
    }
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
