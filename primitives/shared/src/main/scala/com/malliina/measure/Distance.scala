package com.malliina.measure

import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, Reads, Writes}

/**
  *
  * @param millis millimeters
  */
class Distance(millis: Long) {
  private val k = 1000

  def toMillis = millis

  def toMeters = millis / k

  def toMetersDouble = 1.0D * millis / k

  def toKilometers = toMeters / k

  def toKilometersDouble = toMetersDouble / k

  def <(other: Distance) = toMillis < other.toMillis

  def <=(other: Distance) = toMillis <= other.toMillis

  def >(other: Distance) = toMillis > other.toMillis

  def >=(other: Distance) = this.toMillis >= other.toMillis

  def ==(other: Distance) = this.toMillis == other.toMillis

  def !=(other: Distance) = this.toMillis != other.toMillis

  /**
    * @return a string of format 'n units'
    */
  override def toString =
    if (toKilometers > 10) s"$toKilometers km"
    else if (toMeters > 10) s"$toMeters m"
    else s"$toMillis mm"
}

object Distance {
  val empty = new Distance(0)

  implicit val json: Format[Distance] = Format[Distance](
    Reads(_.validate[Long].map(_.millimeters)),
    Writes(size => toJson(size.toMillis))
  )
}
