package com.malliina.measure

class MeasureTests extends munit.FunSuite {
  test("dms to dd") {
    val dms = DecimalDegrees.Dms(74, 0, 21)
    val dd = dms.dd
    assert(dms.dd.absDiff(dd.dms.dd) < 0.01)
  }

  test("dd to dms") {
    val dd = new DecimalDegrees(38.8897)
    val dms = dd.dms
    assert(dms.degree == 38)
    assert(dms.minute == 53)
    assert(math.rint(dms.seconds).toInt == 23)
    assert(dms.dd.absDiff(dd) < 0.001)
  }

  test("degree is zero") {
    val dd = (-74.00583333).dd
    val dms = dd.dms
    assert(dd.dms.degree == -74)
    assert(dms.dd.absDiff(dd) < 0.00001)
  }

  test("second is zero") {
    val dms = DecimalDegrees.Dms(0, 0, -15)
    val dd = dms.dd
    assert(dd.isNegative)
    assert(dd.dms == dms)
  }

  test("minute is zero") {
    val dms = DecimalDegrees.Dms(0, -10, 24)
    val dd = dms.dd
    assert(dd.isNegative)
    assert((dd.dms.dd - dms.dd).dd < 0.001)
  }

  test("knots to meters per second") {
    val ms = new SpeedIntM(3).knots.toMps
    val expected = 1.5433333333333334d
    assert(math.abs(ms - expected) < 0.001)
  }

  test("sort speeds") {
    val s1 = SpeedM(4)
    val s2 = SpeedM(2)
    val s3 = SpeedM(11.4)
    assert(Seq(s1, s2, s3).sorted == Seq(s2, s1, s3))
  }

  test("compare speeds") {
    val s1 = SpeedM(4)
    val s2 = SpeedM(2)
    assert(s1 > s2)
  }
}
