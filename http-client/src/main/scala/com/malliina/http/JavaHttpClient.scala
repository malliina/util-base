package com.malliina.http

import cats.effect.{Async, Resource}
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.Ops.{BodyHandlerOps, CompletionStageOps}
import com.malliina.http.JavaHttpClient.{bodyRequest, jsonBodyRequest, postFormRequest, postJsonRequest, requestFor}
import com.malliina.storage.{StorageLong, StorageSize}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json, parser}
import jdk.internal.net.http.common.Utils.charsetFrom
import cats.syntax.all.catsSyntaxApplicativeError

import java.net.{URI, URLEncoder}
import java.net.http.HttpRequest.{BodyPublisher, BodyPublishers}
import java.net.http.HttpResponse.*
import java.net.http.{HttpRequest, HttpClient as JHttpClient, HttpResponse as JHttpResponse}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.{CompletableFuture, Executor}

object JavaHttpClient extends HttpHeaders {
  def requestFor(url: FullUrl, headers: Map[String, String]): HttpRequest =
    requestBuilder(url, headers).build()

  def postJsonRequest[T: Encoder](url: FullUrl, t: T, headers: Map[String, String]): HttpRequest =
    jsonBodyRequest(HttpMethod.Post, url, t, headers)

  def jsonBodyRequest[T: Encoder](
    method: HttpMethod with BodyMethod,
    url: FullUrl,
    t: T,
    headers: Map[String, String]
  ): HttpRequest = bodyRequest(method, url, jsonBodyPublisher(t), application.json, headers)

  def bodyRequest(
    method: HttpMethod with BodyMethod,
    url: FullUrl,
    body: BodyPublisher,
    contentType: String,
    headers: Map[String, String]
  ): HttpRequest =
    requestBuilder(
      url,
      headers,
      HttpRequest.newBuilder().header(`Content-Type`, contentType)
    ).method(method.name, body).build()

  def postFormRequest(
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String]
  ): HttpRequest = {
    val encoded = form.map { case (k, v) =>
      s"${encode(k)}=${encode(v)}"
    }.mkString("&")
    requestBuilder(
      url,
      headers,
      HttpRequest.newBuilder().header(`Content-Type`, application.form)
    ).method(HttpMethod.Post.name, BodyPublishers.ofString(encoded)).build()
  }

  private def encode(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)

  def requestBuilder(
    url: FullUrl,
    headers: Map[String, String],
    base: HttpRequest.Builder = HttpRequest.newBuilder()
  ): HttpRequest.Builder =
    headers
      .foldLeft(base) { case (b, (k, v)) =>
        b.header(k, v)
      }

  private def jsonBodyPublisher[T: Encoder](t: T) = BodyPublishers.ofString(t.asJson.noSpaces)

}

class JavaHttpClient[F[_]: Async](javaHttp: JHttpClient) extends HttpClient[F] {
  private val F = Async[F]

  def getAs[T: Decoder](url: FullUrl, headers: Map[String, String] = Map.empty): F[T] =
    fetchJson[T](requestFor(url, headers), url)

  def get(url: FullUrl, headers: Map[String, String] = Map.empty): F[HttpResponse] =
    fetchString(requestFor(url, headers))

  def postAs[W: Encoder, T: Decoder](
    url: FullUrl,
    json: W,
    headers: Map[String, String] = Map.empty
  ): F[T] = fetchJson[T](postJsonRequest(url, json, headers), url)

  def postJson(
    url: FullUrl,
    json: Json,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse] =
    fetchString(postJsonRequest[Json](url, json, headers))

  def putJson(
    url: FullUrl,
    json: Json,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse] =
    fetchString(jsonBodyRequest[Json](HttpMethod.Put, url, json, headers))

  def putAs[W: Encoder, T: Decoder](
    url: FullUrl,
    json: W,
    headers: Map[String, String] = Map.empty
  ): F[T] = fetchJson[T](jsonBodyRequest(HttpMethod.Put, url, json, headers), url)

  def postFormAs[T: Decoder](
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String] = Map.empty
  ): F[T] =
    fetchJson[T](postFormRequest(url, form, headers), url)

  def postForm(
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse] =
    fetchString(postFormRequest(url, form, headers))

  def postFile(
    url: FullUrl,
    mediaType: String,
    file: Path,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse] = fetchString(
    bodyRequest(HttpMethod.Post, url, BodyPublishers.ofFile(file), mediaType, headers)
  )

  def download(
    url: FullUrl,
    to: Path,
    headers: Map[String, String] = Map.empty
  ): F[Either[StatusError, StorageSize]] =
    downloadFile(url, to, headers)
      .map[Either[StatusError, StorageSize]] { path =>
        Right(Files.size(path).bytes)
      }
      .recoverWith { case re: ResponseException =>
        re.error match {
          case se: StatusError => Async[F].pure(Left(se))
          case other           => Async[F].raiseError(re)
        }
      }

  def downloadFile(url: FullUrl, to: Path, headers: Map[String, String] = Map.empty): F[Path] =
    fetchFold(
      requestFor(url, headers),
      okBodyParser(url, BodyHandlers.ofFile(to))
    )

  def fetchBytes(url: FullUrl, headers: Map[String, String]): F[Array[Byte]] =
    fetchFold(requestFor(url, headers), okBodyParser(url, BodyHandlers.ofByteArray()))

  def socket(url: FullUrl, headers: Map[String, String], executor: Executor): Resource[F, WebSocket[F]] =
    WebSocket.build[F](url, headers, javaHttp, executor)

  private def fetchJson[T: Decoder](request: HttpRequest, url: FullUrl): F[T] =
    fetchFold(request, jsonBodyParser[T](url))

  private def fetchString(request: HttpRequest): F[HttpResponse] =
    fetch(request, BodyHandlers.ofString()).map(res => new StringResponse(res))

  private def fetchFold[T](
    request: HttpRequest,
    handler: JHttpResponse.BodyHandler[Either[ResponseError, T]]
  ): F[T] = fetch(request, handler).flatMap[T] { res =>
    res.body().fold(err => fail(err), t => F.pure(t))
  }

  private def fetch[T](
    request: HttpRequest,
    handler: JHttpResponse.BodyHandler[T]
  ): F[JHttpResponse[T]] =
    javaHttp.sendAsync(request, handler).effect[F]

  private def jsonBodyParser[T: Decoder](
    url: FullUrl
  ): BodyHandler[Either[ResponseError, T]] =
    bodyParser(url) { res =>
      BodySubscribers.mapping(
        BodySubscribers.ofString(charsetFrom(res.headers())),
        (str: String) =>
          parser
            .decode(str)
            .fold(err => Left(JsonError(err, new JavaResponseMeta(res), url)), t => Right(t))
      )
    }

  private def okBodyParser[T](url: FullUrl, parser: BodyHandler[T]) =
    bodyParser(url)(parser.map[Either[ResponseError, T]](t => Right(t)))

  private def bodyParser[T](url: FullUrl)(
    successParser: BodyHandler[Either[ResponseError, T]]
  ): BodyHandler[Either[ResponseError, T]] =
    (res: ResponseInfo) => {
      val meta = new JavaResponseMeta(res)
      if (meta.isSuccess) {
        successParser(res)
      } else {
        BodyHandlers
          .ofString()
          .map[Either[ResponseError, T]](str =>
            Left(StatusError(new JavaBodyResponse(meta, str), url))
          )(res)
      }
    }

  private def fail[T](error: ResponseError): F[T] = F.raiseError(error.toException)
}

