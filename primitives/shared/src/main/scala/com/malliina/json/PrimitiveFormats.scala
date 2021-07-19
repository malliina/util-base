package com.malliina.json

import io.circe._

import scala.concurrent.duration.{Duration, DurationDouble}

object PrimitiveFormats {
  implicit val durationCodec: Codec[Duration] = Codec.from(
    Decoder.decodeDouble.map(_.seconds),
    Encoder.encodeDouble.contramap[Duration](_.toSeconds.toDouble)
  )
}
