package com.malliina.measure

import com.malliina.measure.SpeedM.{knotInKmh, meterPerSecondInKmh}
import io.circe._

/** @param mps
  *   meters per second
  */
case class SpeedM(mps: Double) extends AnyVal with Ordered[SpeedM] {
  override def compare(that: SpeedM): Int = mps compare that.toMps

  def toMps = mps
  def toKmh: Double = mps * meterPerSecondInKmh
  def toKnots = toKmh / knotInKmh

  def +(other: SpeedM): SpeedM = SpeedM(toMps + other.toMps)
  def -(other: SpeedM): SpeedM = SpeedM(toMps - other.toMps)
  def ==(other: SpeedM) = this.toMps == other.toMps
  def !=(other: SpeedM) = this.toMps != other.toMps

  def formatMs = s"$toMps m/s"

  /** @return
    *   a string of format 'n units'
    */
  def formatKmh = s"$toKmh kmh"
  def formatKnots = s"$toKnots kn"

  override def toString = formatMs
}

object SpeedM {
  val zero = new SpeedM(0)

  val knotInKmh = 1.852d
  val meterPerSecondInKmh = 3.6d

  val kmhJson: Codec[SpeedM] = Codec.from(
    Decoder.decodeDouble.map(kmh => apply(kmh / meterPerSecondInKmh)),
    Encoder.encodeDouble.contramap(_.toKmh)
  )

  val mpsJson: Codec[SpeedM] = Codec.from(
    Decoder.decodeDouble.map(mps => apply(mps)),
    Encoder.encodeDouble.contramap(_.toMps)
  )

  // Not the best default codec, but stays like this for compatibility
  implicit val knotsJson: Codec[SpeedM] = Codec.from(
    Decoder.decodeDouble.map(kn => apply(kn * knotInKmh / meterPerSecondInKmh)),
    Encoder.encodeDouble.contramap(_.toKnots)
  )
}
