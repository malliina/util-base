package com.malliina.values

import com.malliina.values.Literals.{err, error}

class PrimitivesTests extends munit.FunSuite:
  test("Construct error with macro"):
    val msg = err"Wrong input"
    assertEquals(msg, ErrorMessage("Wrong input"))

  test("Construct error with interpolation"):
    val name = "Jack"
    val msg = error"Bad, $name"
    assertEquals(msg, ErrorMessage("Bad, Jack"))
