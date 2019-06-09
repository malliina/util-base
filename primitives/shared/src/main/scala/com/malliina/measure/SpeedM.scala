package com.malliina.measure

import com.malliina.measure.Speed.{knotInKmh, meterPerSecondInKmh}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, Reads, Writes}

/**
  * @param mps meters per second
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

  /**
    * @return a string of format 'n units'
    */
  def formatKmh = s"$toKmh kmh"

  def formatKnots = s"$toKnots kn"

  override def toString = formatMs
}

object SpeedM {
  val zero = new SpeedM(0)

  val knotInKmh = 1.852D
  val meterPerSecondInKmh = 3.6D

  val kmhJson: Format[SpeedM] = Format[SpeedM](
    Reads(_.validate[Double].map(kmh => apply(kmh / meterPerSecondInKmh))),
    Writes(size => toJson(size.toKmh))
  )

  implicit val knotsJson: Format[SpeedM] = Format[SpeedM](
    Reads(_.validate[Double].map(kn => apply(kn * knotInKmh / meterPerSecondInKmh))),
    Writes(size => toJson(size.toKnots))
  )

  /**
    * @param mps meters per second
    */
  def apply(mps: Double): SpeedM = new SpeedM(mps)
}
