package com.malliina.json

import play.api.libs.json.Json.toJson
import play.api.libs.json._

import scala.concurrent.duration.{Duration, DurationDouble}

object PrimitiveFormats {
  implicit val durationFormat: Format[Duration] = Format[Duration](
    Reads(_.validate[Double].map(_.seconds)),
    Writes(d => toJson(d.toSeconds))
  )
}
