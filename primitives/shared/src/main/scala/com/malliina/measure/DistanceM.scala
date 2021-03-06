package com.malliina.measure

import com.malliina.measure.DistanceM.k
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, Reads, Writes}

/**
  *
  * @param meters meters
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

  /**
    * @return a string of format 'n units'
    */
  def short: String =
    if (toKilometers >= 10) s"$toKilometers km"
    else if (toMeters >= 10) s"$toMeters m"
    else s"$toMillis mm"

  /**
    * @return a string of format 'n units'
    */
  override def toString = short
}

object DistanceM {
  val zero = new DistanceM(0)
  private val k = 1000

  def apply(meters: Double): DistanceM = new DistanceM(meters)

  implicit val json: Format[DistanceM] = Format[DistanceM](
    Reads(_.validate[Double].map(m => DistanceM(m))),
    Writes(size => toJson(size.toMeters))
  )
}
