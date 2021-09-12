package com.malliina

package object measure {
  private val k = 1000L

  /**
    * @param amount integer amount of some distance unit
    */
  implicit final class DistanceIntM(val amount: Int) extends AnyVal with DistanceConversionsM {
    protected def asMeters(multiplier: Double): Double = multiplier * amount
  }

  implicit final class DistanceLongM(val amount: Long) extends AnyVal with DistanceConversionsM {
    protected def asMeters(multiplier: Double): Double = multiplier * amount
  }

  implicit final class DistanceDoubleM(val amount: Double)
    extends AnyVal
    with DistanceConversionsM {
    protected def asMeters(multiplier: Double): Double = multiplier * amount
  }

  trait DistanceConversionsM extends Any {
    protected def asDistance(multiplier: Double): DistanceM = new DistanceM(asMeters(multiplier))
    protected def asMeters(multiplier: Double): Double

    def mm = millimeters
    def millimeters = asDistance(1.0d / k.toDouble)
    def m = meters
    def meters = asDistance(1)
    def km = kilometers
    def kilometers = asDistance(k.toDouble)
  }

  implicit final class SpeedIntM(private val amount: Int) extends AnyVal with SpeedConversionsM {
    protected def asMps(multiplier: Double): Double = multiplier * amount
  }

  implicit final class SpeedLongM(private val amount: Long) extends AnyVal with SpeedConversionsM {
    protected def asMps(multiplier: Double): Double = multiplier * amount
  }

  implicit final class SpeedDoubleM(private val amount: Double)
    extends AnyVal
    with SpeedConversionsM {
    protected def asMps(multiplier: Double): Double = multiplier * amount
  }

  trait SpeedConversionsM extends Any {
    protected def asSpeed(multiplier: Double): SpeedM = new SpeedM(asMps(multiplier))

    protected def asMps(multiplier: Double): Double

    def `m/s`: SpeedM = metersPerSecond
    def mps = metersPerSecond
    def metersPerSecond = asSpeed(1)
    def kmh = asSpeed(1d / SpeedM.meterPerSecondInKmh)
    def kn = knots
    def knots = asSpeed(1d / SpeedM.meterPerSecondInKmh * SpeedM.knotInKmh)
  }

  implicit final class TemperatureInt(private val amount: Int)
    extends AnyVal
    with TemperatureConversions {
    def celsius = asCelsius(amount)
    def fahrenheit = fromFahrenheit(amount)
    def kelvin = fromKelvin(amount)
  }

  implicit final class TemperatureLong(private val amount: Long)
    extends AnyVal
    with TemperatureConversions {
    def celsius = asCelsius(amount.toDouble)
    def fahrenheit = fromFahrenheit(amount.toDouble)
    def kelvin = fromKelvin(amount.toDouble)
  }

  implicit final class TemperatureDouble(private val amount: Double)
    extends AnyVal
    with TemperatureConversions {
    def celsius = asCelsius(amount)
    def fahrenheit = fromFahrenheit(amount)
    def kelvin = fromKelvin(amount)
  }

  trait TemperatureConversions extends Any {
    protected def asCelsius(celsius: Double): Temperature = new Temperature(celsius)
    protected def fromFahrenheit(f: Double) = asCelsius(Temperature.fahrenheitToCelsius(f))
    protected def fromKelvin(k: Double) = asCelsius(Temperature.kelvinToCelsius(k))
  }

  implicit final class DegreeDouble(private val amount: Double)
    extends AnyVal
    with DegreeConversions {
    def dd = asDegree(amount)
  }

  implicit final class DegreeInt(private val amount: Int) extends AnyVal with DegreeConversions {
    def dd = asDegree(amount.toDouble)
  }

  implicit final class DegreeLong(private val amount: Long) extends AnyVal with DegreeConversions {
    def dd = asDegree(amount.toDouble)
  }

  trait DegreeConversions extends Any {
    def asDegree(dd: Double): DecimalDegrees = new DecimalDegrees(dd)
  }
}
