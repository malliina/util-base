package com.malliina.json

import io.circe._

import scala.concurrent.duration.{Duration, DurationDouble}

object PrimitiveFormats {
  val durationEncoder: Encoder[Duration] = Encoder.encodeDouble.contramap[Duration](_.toSeconds)
  val durationDecoder: Decoder[Duration] = Decoder.decodeDouble.map(_.seconds)
  implicit val durationCodec: Codec[Duration] = Codec.from(durationDecoder, durationEncoder)
}
