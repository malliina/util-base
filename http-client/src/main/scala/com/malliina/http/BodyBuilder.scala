package com.malliina.http

import io.circe.Json

import java.net.http.HttpRequest.{BodyPublisher, BodyPublishers}

trait BodyBuilder[T, B] {
  def build(t: T): B
  def contentType: String
}

trait JavaBodyBuilder[T] extends BodyBuilder[T, BodyPublisher]

object JavaBodyBuilder {
  def apply[T](implicit jbb: JavaBodyBuilder[T]): JavaBodyBuilder[T] = jbb
  implicit val json: JavaBodyBuilder[Json] = new JavaBodyBuilder[Json] {
    override def build(t: Json): BodyPublisher = BodyPublishers.ofString(t.noSpaces)
    override def contentType: String = HttpHeaders.application.json
  }
}
