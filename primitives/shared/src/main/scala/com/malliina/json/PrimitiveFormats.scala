package com.malliina.json

import io.circe._
import scala.concurrent.duration.{Duration, DurationDouble}

object PrimitiveFormats {
  val durationEncoder = Encoder.encodeDouble.contramap[Duration](_.toSeconds)
  val durationDecoder = Decoder.decodeDouble.map(_.seconds)
}
