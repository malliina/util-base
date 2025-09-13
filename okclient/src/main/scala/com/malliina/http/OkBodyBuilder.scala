package com.malliina.http

import io.circe.Json
import okhttp3.RequestBody

trait OkBodyBuilder[T] extends BodyBuilder[T, RequestBody]

object OkBodyBuilder {
  implicit val json: OkBodyBuilder[Json] = new OkBodyBuilder[Json] {
    override def build(t: Json): RequestBody =
      RequestBody.create(t.noSpaces, OkClient.jsonMediaType)
    override def contentType: String = OkClient.jsonMediaType.toString
  }
}
