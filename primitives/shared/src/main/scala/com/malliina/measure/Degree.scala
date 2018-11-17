package com.malliina.measure

import com.malliina.measure.Degree.Dms

class Degree(val dd: Double) extends Ordered[Degree] {
  override def compare(that: Degree): Int = dd compare that.dd

  def dms: Dms = {
    val d = dd.toInt
    val m = ((math.abs(dd) * 60) % 60).toInt
    val mSign = if (isNegative && d == 0) -1 else 1
    val s = (math.abs(dd) * 3600) % 60
    val sSign = if (isNegative && d == 0 && m == 0) -1 else 1
    Dms(dd.toInt, m * mSign, s * sSign)
  }

  def +(other: Degree): Degree = (dd + other.dd).dd

  def +(other: Dms): Degree = this + other.dd

  def -(other: Degree): Degree = (dd - other.dd).dd

  def -(other: Dms): Degree = this - other.dd

  def ==(other: Degree): Boolean = dd == other.dd

  def ==(other: Dms): Boolean = this == other.dd

  def !=(other: Degree): Boolean = dd != other.dd

  def !=(other: Dms): Boolean = this != other.dd

  def isNegative = dd < 0

  override def toString: String = s"$dd"
}

object Degree {

  /**
    * @see http://www.mathworks.com/help/map/ref/dms2degrees.html?w.mathworks.com
    */
  case class Dms(degree: Int, minute: Int, seconds: Double) {
    // true if the first nonzero element of degree, minute, seconds is negative
    def isNegative = degree < 0 || (degree == 0 && minute < 0) || (degree == 0 && minute == 0 && seconds < 0)

    def dd: Degree = {
      val degrees = math.abs(degree) + math.abs(minute.toDouble) / 60d + math.abs(seconds.toDouble) / 3600d
      new Degree(if (isNegative) -degrees else degrees)
    }
  }

}
