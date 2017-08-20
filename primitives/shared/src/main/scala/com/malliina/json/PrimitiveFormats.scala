package com.malliina.json

import play.api.libs.json.Json.toJson
import play.api.libs.json._

import scala.concurrent.duration.{Duration, DurationLong}

object PrimitiveFormats {
  implicit val durationFormat: Format[Duration] = Format[Duration](
    Reads(_.validate[Long].map(_.seconds)),
    Writes(d => toJson(d.toSeconds))
  )
}
