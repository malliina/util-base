package com.malliina.measure

import com.malliina.measure.DistanceM.k
import io.circe.{Codec, Decoder, Encoder}

/** @param meters
  *   meters
  */
case class DistanceM(meters: Double) extends AnyVal with Ordered[DistanceM] {
  override def compare(that: DistanceM): Int = toMillis compare that.toMillis

  def toMillis = meters * k
  def toMeters = meters
  def toKilometers = meters / k

  def +(other: DistanceM) = DistanceM(toMeters + other.toMeters)
  def -(other: DistanceM) = DistanceM(toMeters - other.toMeters)
  def ==(other: DistanceM) = this.toMeters == other.toMeters
  def !=(other: DistanceM) = this.toMeters != other.toMeters

  /** @return
    *   a string of format 'n units'
    */
  def short: String =
    if (toKilometers >= 10) s"$toKilometers km"
    else if (toMeters >= 10) s"$toMeters m"
    else s"$toMillis mm"

  /** @return
    *   a string of format 'n units'
    */
  override def toString = short
}

object DistanceM {
  val zero = new DistanceM(0)
  private val k = 1000

  implicit val json: Codec[DistanceM] = Codec.from(
    Decoder.decodeDouble.map(m => DistanceM(m)),
    Encoder.encodeDouble.contramap(_.toMeters)
  )
  implicit val numeric: Numeric[DistanceM] = Numerical[Double, DistanceM](apply, _.meters)
}
