package com.malliina.measure

import org.scalatest.FunSuite

class MeasureTests extends FunSuite {
  test("dms to dd") {
    val dms = Degree.Dms(74, 0, 21)
    val dd = dms.dd
    assert((dms.dd - dd.dms.dd).dd < 0.01)
  }

  test("dd to dms") {
    val dd = new Degree(38.8897)
    val dms = dd.dms
    assert(dms.degree === 38)
    assert(dms.minute === 53)
    assert(math.rint(dms.seconds).toInt === 23)
    assert((dms.dd - dd).dd < 0.001)
  }

  test("degree is zero") {
    val dd = (-74.00583333).dd
    val dms = dd.dms
    assert(dd.dms.degree === -74)
    assert((dms.dd - dd).dd < 0.00001)
  }

  test("second is zero") {
    val dms = Degree.Dms(0, 0, -15)
    val dd = dms.dd
    assert(dd.isNegative)
    assert(dd.dms === dms)
  }

  test("minute is zero") {
    val dms = Degree.Dms(0, -10, 24)
    val dd = dms.dd
    assert(dd.isNegative)
    assert((dd.dms.dd - dms.dd).dd < 0.001)
  }
}
