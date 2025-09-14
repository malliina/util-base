package com.malliina.http

import cats.effect.{Async, Resource, Sync}
import com.malliina.storage.StorageSize
import io.circe.{Decoder, Encoder, Json}

import java.net.http.{HttpClient => JHttpClient}
import java.nio.file.Path

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
  def getAs[T: Decoder](url: FullUrl, headers: Map[String, String]): F[T]

  def getAs[T: Decoder](url: FullUrl): F[T] = getAs(url, Map.empty)

  def get(url: FullUrl, headers: Map[String, String]): F[HttpResponse]

  def get(url: FullUrl): F[HttpResponse] = get(url, Map.empty)

  def postAs[W: Encoder, T: Decoder](
    url: FullUrl,
    json: W,
    headers: Map[String, String] = Map.empty
  ): F[T]

  def putAs[W: Encoder, T: Decoder](
    url: FullUrl,
    json: W,
    headers: Map[String, String] = Map.empty
  ): F[T]

  def postFormAs[T: Decoder](
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String] = Map.empty
  ): F[T]

  def postString(
    url: FullUrl,
    string: String,
    mediaType: String,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse]

  def postJson(
    url: FullUrl,
    json: Json,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse] = postString(url, json.noSpaces, HttpHeaders.application.json, headers)

  def putJson(
    url: FullUrl,
    json: Json,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse]

  def postFile(
    url: FullUrl,
    mediaType: String,
    file: Path,
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse]

  def postForm(
    url: FullUrl,
    form: Map[String, String],
    headers: Map[String, String] = Map.empty
  ): F[HttpResponse]

  def downloadFile(url: FullUrl, to: Path, headers: Map[String, String] = Map.empty): F[Path]

  def download(
    url: FullUrl,
    to: Path,
    headers: Map[String, String] = Map.empty
  ): F[Either[StatusError, StorageSize]]
}

trait HttpClient[F[_]] extends SimpleHttpClient[F] {
  type Socket <: WebSocketOps[F]

  def socket(
    url: FullUrl,
    headers: Map[String, String]
  ): Resource[F, ReconnectingSocket[F, Socket]]
}
