package com.malliina.measure

import io.circe._

/** @param celsius
  *   degrees in Celsius scale
  */
case class Temperature(celsius: Double) extends AnyVal with Ordered[Temperature] {
  override def compare(that: Temperature): Int = toCelsius compare that.toCelsius

  def toCelsius = celsius
  def toFahrenheit = Temperature.celsiusToFahrenheit(celsius)
  def toKelvin = Temperature.celsiusToKelvin(celsius)

  def +(other: Temperature): Temperature = (toCelsius + other.toCelsius).celsius
  def -(other: Temperature): Temperature = (toCelsius - other.toCelsius).celsius
  def ==(other: Temperature) = toCelsius == other.toCelsius
  def !=(other: Temperature) = toCelsius != other.toCelsius

  /** @return
    *   a string of format 'n units'
    */
  def formatCelsius = s"$toCelsius C"

  override def toString = formatCelsius
}

object Temperature {
  private val kelvinDiff = 273.15d
  val zeroCelsius = new Temperature(0)
  val absoluteZero = new Temperature(-kelvinDiff)

  implicit val celsiusJson: Codec[Temperature] = Codec.from(
    Decoder.decodeDouble.map(_.celsius),
    Encoder.encodeDouble.contramap[Temperature](_.toCelsius)
  )

  val fahrenheitJson = Codec.from(
    Decoder.decodeDouble.map(_.fahrenheit),
    Encoder.encodeDouble.contramap[Temperature](_.toFahrenheit)
  )

  def celsiusToFahrenheit(c: Double): Double = c * 9 / 5 + 32
  def fahrenheitToCelsius(f: Double): Double = (f - 32) * 5 / 9
  def kelvinToCelsius(k: Double): Double = k - kelvinDiff
  def celsiusToKelvin(c: Double): Double = c + kelvinDiff
}
