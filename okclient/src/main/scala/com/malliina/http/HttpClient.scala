package com.malliina.http

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, StandardCopyOption}

import com.malliina.http.OkClient.MultiPartFile
import com.malliina.storage.{StorageLong, StorageSize}
import okhttp3._
import play.api.libs.json.{JsValue, Json, Reads, Writes}

trait HttpClient[F[_]] {
  implicit class FOps[T](f: F[T]) {
    def flatMap[U](code: T => F[U]): F[U] = HttpClient.this.flatMap(f)(code)
  }

  def getAs[T: Reads](url: FullUrl, headers: Map[String, String] = Map.empty): F[T] =
    get(url, headers).flatMap(r => parse[T](r, url))

  def get(url: FullUrl, headers: Map[String, String] = Map.empty): F[OkHttpResponse] = {
    val req = requestFor(url, headers)
    execute(req.get().build())
  }

  def postAs[W: Writes, T: Reads](
    url: FullUrl,
    json: W,
    headers: Map[String, String] = Map.empty
  ): F[T] =
    postJson(url, Json.toJson(json), headers).flatMap(r => parse[T](r, url))

  def postJsonAs[T: Reads](
    url: FullUrl,
    json: JsValue,
    headers: Map[String, String] = Map.empty
  ): F[T] =
    postJson(url, json, headers).flatMap(r => parse[T](r, url))

  def postFormAs[T: Reads](
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String] = Map.empty
  ): F[T] =
    postForm(url, form, headers).flatMap(r => parse[T](r, url))

  def postJson(
    url: FullUrl,
    json: JsValue,
    headers: Map[String, String] = Map.empty
  ): F[OkHttpResponse] =
    post(url, RequestBody.create(Json.stringify(json), OkClient.jsonMediaType), headers)

  def postFile(
    url: FullUrl,
    mediaType: MediaType,
    file: Path,
    headers: Map[String, String] = Map.empty
  ): F[OkHttpResponse] =
    post(url, RequestBody.create(file.toFile, mediaType), headers)

  def postForm(
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String] = Map.empty
  ): F[OkHttpResponse] = {
    val bodyBuilder = new FormBody.Builder(StandardCharsets.UTF_8)
    form foreach {
      case (k, v) =>
        bodyBuilder.add(k, v)
    }
    post(url, bodyBuilder.build(), headers)
  }

  def multiPart(
    url: FullUrl,
    headers: Map[String, String] = Map.empty,
    parts: Map[String, String] = Map.empty,
    files: Seq[MultiPartFile] = Nil
  ): F[OkHttpResponse] = {
    val bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM)
    parts.foreach {
      case (k, v) =>
        bodyBuilder.addFormDataPart(k, v)
    }
    files.foreach { filePart =>
      bodyBuilder.addFormDataPart(
        filePart.partName,
        filePart.fileName,
        RequestBody.create(filePart.file.toFile, filePart.mediaType)
      )
    }
    post(url, bodyBuilder.build(), headers)
  }

  def post(
    url: FullUrl,
    body: RequestBody,
    headers: Map[String, String]
  ): F[OkHttpResponse] = {
    val builder = requestFor(url, headers).post(body)
    execute(builder.build())
  }

  /** Downloads `url` to `to`, returning the number of bytes written to `to`.
    *
    * @param url     url to download
    * @param to      destination, a file
    * @param headers http headers
    * @return bytes written
    */
  def download(
    url: FullUrl,
    to: Path,
    headers: Map[String, String] = Map.empty
  ): F[Either[StatusError, StorageSize]] =
    streamed(requestFor(url, headers).get().build()) { response =>
      success {
        if (response.isSuccessful)
          Right(
            Files.copy(response.body().byteStream(), to, StandardCopyOption.REPLACE_EXISTING).bytes
          )
        else Left(StatusError(OkHttpResponse(response), url))
      }
    }

  def streamed[T](request: Request)(consume: Response => F[T]): F[T]

  def execute(request: Request): F[OkHttpResponse]

  def raw(request: Request): F[Response]

  def requestFor(url: FullUrl, headers: Map[String, String]) =
    headers.foldLeft(new Request.Builder().url(url.url)) {
      case (r, (key, value)) => r.addHeader(key, value)
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
  def parse[T: Reads](response: OkHttpResponse, url: FullUrl): F[T] =
    if (response.isSuccess) {
      response
        .parse[T]
        .fold(
          err => fail(JsonError(err, response, url).toException),
          ok => success(ok)
        )
    } else {
      fail(StatusError(response, url).toException)
    }

  def flatMap[T, U](t: F[T])(f: T => F[U]): F[U]
  def success[T](t: T): F[T]
  def fail[T](e: Exception): F[T]
}
