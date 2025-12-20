package com.malliina.http

import cats.effect.{Async, Resource, Sync}
import com.malliina.storage.StorageSize
import io.circe.{Decoder, Encoder, Json}

import java.net.http.{HttpClient => JHttpClient}
import java.nio.file.Path
import scala.concurrent.duration.FiniteDuration

object HttpClient {
  def javaResource[F[_]: Sync](
    builder: JHttpClient.Builder
  ): Resource[F, JHttpClient] =
    Resource.make[F, JHttpClient](Sync[F].delay(builder.build()))(c => Sync[F].delay(()))
  def resource[F[_]: Async](
    builder: JHttpClient.Builder = JHttpClient.newBuilder(),
    defaultHeaders: Map[String, String] = Map.empty
  ): Resource[F, JavaHttpClient[F]] =
    javaResource(builder).map(javaClient => new JavaHttpClient(javaClient, defaultHeaders))
}

trait SimpleHttpClient[F[_]] {
  def getAs[T: Decoder](url: FullUrl): F[T] = getAs[T](url, Map.empty[String, String])

  def getAs[T: Decoder](url: FullUrl, headers: Map[String, String]): F[T]

  def get(url: FullUrl): F[HttpResponse] = get(url, Map.empty)

  def get(url: FullUrl, headers: Map[String, String]): F[HttpResponse]

  def postAs[W: Encoder, T: Decoder](url: FullUrl, json: W): F[T] =
    postAs[W, T](url, json, Map.empty[String, String])

  def postAs[W: Encoder, T: Decoder](url: FullUrl, json: W, headers: Map[String, String]): F[T]

  def putAs[W: Encoder, T: Decoder](url: FullUrl, json: W): F[T] =
    putAs[W, T](url, json, Map.empty[String, String])

  def putAs[W: Encoder, T: Decoder](url: FullUrl, json: W, headers: Map[String, String]): F[T]

  def postFormAs[T: Decoder](url: FullUrl, form: Map[String, String]): F[T] =
    postFormAs[T](url, form, Map.empty[String, String])

  def postFormAs[T: Decoder](
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String]
  ): F[T]

  def postString(url: FullUrl, string: String, mediaType: String): F[HttpResponse] =
    postString(url, string, mediaType, Map.empty)

  def postString(
    url: FullUrl,
    string: String,
    mediaType: String,
    headers: Map[String, String]
  ): F[HttpResponse]

  def postJson(url: FullUrl, json: Json): F[HttpResponse] =
    postJson(url, json, Map.empty)

  def postJson(
    url: FullUrl,
    json: Json,
    headers: Map[String, String]
  ): F[HttpResponse] = postString(url, json.noSpaces, HttpHeaders.application.json, headers)

  def putJson(url: FullUrl, json: Json): F[HttpResponse] =
    putJson(url, json, Map.empty)

  def putJson(url: FullUrl, json: Json, headers: Map[String, String]): F[HttpResponse]

  def postFile(url: FullUrl, mediaType: String, file: Path): F[HttpResponse] =
    postFile(url, mediaType, file, Map.empty)

  def postFile(
    url: FullUrl,
    mediaType: String,
    file: Path,
    headers: Map[String, String]
  ): F[HttpResponse]

  def postForm(url: FullUrl, form: Map[String, String]): F[HttpResponse] =
    postForm(url, form, Map.empty)

  def postForm(
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String]
  ): F[HttpResponse]

  def postBytes(url: FullUrl, bytes: Array[Byte]): F[HttpResponse] =
    postBytes(url, bytes, Map.empty)

  def postBytes(url: FullUrl, bytes: Array[Byte], headers: Map[String, String]): F[HttpResponse]

  def downloadFile(url: FullUrl, to: Path, headers: Map[String, String]): F[Path]

  def download(url: FullUrl, to: Path): F[Either[StatusError, StorageSize]] =
    download(url, to, Map.empty)

  def download(
    url: FullUrl,
    to: Path,
    headers: Map[String, String]
  ): F[Either[StatusError, StorageSize]]
}

trait HttpClient[F[_]] extends SimpleHttpClient[F] {
  type Socket <: WebSocketOps[F]

  def socket(
    url: FullUrl,
    headers: Map[String, String]
  ): Resource[F, ReconnectingSocket[F, Socket]] = socket(url, headers, WebSocket.DefaultBackOff)

  def socket(
    url: FullUrl,
    headers: Map[String, String],
    backoffTime: FiniteDuration
  ): Resource[F, ReconnectingSocket[F, Socket]]
}
