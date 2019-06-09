package com.malliina

package object measure {
  private val k = 1000L

  /**
    * @param amount integer amount of some distance unit
    */
  implicit final class DistanceInt(val amount: Int) extends AnyVal with DistanceConversions {
    protected def asMillis(multiplier: Long): Long = multiplier * amount
  }

  implicit final class DistanceLong(val amount: Long) extends AnyVal with DistanceConversions {
    protected def asMillis(multiplier: Long): Long = multiplier * amount
  }

  implicit final class DistanceDouble(val amount: Double) extends AnyVal with DistanceConversions {
    protected def asMillis(multiplier: Long): Long = (multiplier * amount).toLong
  }

  trait DistanceConversions extends Any {
    protected def asDistance(multiplier: Long): Distance = new Distance(asMillis(multiplier))

    protected def asMillis(multiplier: Long): Long

    def mm = asDistance(1)

    def millimeters = asDistance(1)

    def m = asDistance(k)

    def meters = asDistance(k)

    def km = asDistance(k * k)

    def kilometers = asDistance(k * k)
  }

  implicit final class DistanceIntM(val amount: Int) extends AnyVal with DistanceConversionsM {
    protected def asMeters(multiplier: Double): Double = multiplier * amount
  }

  implicit final class DistanceLongM(val amount: Long) extends AnyVal with DistanceConversionsM {
    protected def asMeters(multiplier: Double): Double = multiplier * amount
  }

  implicit final class DistanceDoubleM(val amount: Double) extends AnyVal with DistanceConversionsM {
    protected def asMeters(multiplier: Double): Double = multiplier * amount
  }

  trait DistanceConversionsM extends Any {
    protected def asDistance(multiplier: Double): DistanceM = new DistanceM(asMeters(multiplier))

    protected def asMeters(multiplier: Double): Double

    def mm = millimeters

    def millimeters = asDistance(1.0d / k)

    def m = meters

    def meters = asDistance(1)

    def km = kilometers

    def kilometers = asDistance(k)
  }

  implicit final class SpeedInt(private val amount: Int) extends AnyVal with SpeedConversions {
    protected def asKmh(multiplier: Double): Double = multiplier * amount
  }

  implicit final class SpeedLong(private val amount: Long) extends AnyVal with SpeedConversions {
    protected def asKmh(multiplier: Double): Double = multiplier * amount
  }

  implicit final class SpeedDouble(private val amount: Double) extends AnyVal with SpeedConversions {
    protected def asKmh(multiplier: Double): Double = (multiplier * amount).toLong
  }

  trait SpeedConversions extends Any {
    protected def asSpeed(multiplier: Double): Speed = new Speed(asKmh(multiplier))

    protected def asKmh(multiplier: Double): Double

    def `m/s` = metersPerSecond

    def metersPerSecond = asSpeed(Speed.meterPerSecondInKmh)

    def kmh = asSpeed(1)

    def kn = knots

    def knots = asSpeed(Speed.knotInKmh)
  }

  implicit final class SpeedIntM(private val amount: Int) extends AnyVal with SpeedConversionsM {
    protected def asMps(multiplier: Double): Double = multiplier * amount
  }

  implicit final class SpeedLongM(private val amount: Long) extends AnyVal with SpeedConversionsM {
    protected def asMps(multiplier: Double): Double = multiplier * amount
  }

  implicit final class SpeedDoubleM(private val amount: Double) extends AnyVal with SpeedConversionsM {
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

  implicit final class TemperatureInt(private val amount: Int) extends AnyVal with TemperatureConversions {
    def celsius = asCelsius(amount)

    def fahrenheit = fromFahrenheit(amount)

    def kelvin = fromKelvin(amount)
  }

  implicit final class TemperatureLong(private val amount: Long) extends AnyVal with TemperatureConversions {
    def celsius = asCelsius(amount)

    def fahrenheit = fromFahrenheit(amount)

    def kelvin = fromKelvin(amount)
  }

  implicit final class TemperatureDouble(private val amount: Double) extends AnyVal with TemperatureConversions {
    def celsius = asCelsius(amount)

    def fahrenheit = fromFahrenheit(amount)

    def kelvin = fromKelvin(amount)
  }

  trait TemperatureConversions extends Any {
    protected def asCelsius(celsius: Double): Temperature = new Temperature(celsius)

    protected def fromFahrenheit(f: Double) = asCelsius(Temperature.fahrenheitToCelsius(f))

    protected def fromKelvin(k: Double) = asCelsius(Temperature.kelvinToCelsius(k))
  }

  implicit final class DegreeDouble(private val amount: Double) extends AnyVal with DegreeConversions {
    def dd = asDegree(amount)
  }

  implicit final class DegreeInt(private val amount: Int) extends AnyVal with DegreeConversions {
    def dd = asDegree(amount)
  }

  implicit final class DegreeLong(private val amount: Long) extends AnyVal with DegreeConversions {
    def dd = asDegree(amount)
  }

  trait DegreeConversions extends Any {
    def asDegree(dd: Double): DecimalDegrees = new DecimalDegrees(dd)
  }
}
