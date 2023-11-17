package com.malliina.config

import cats.data.NonEmptyList
import com.typesafe.config.ConfigFactory

class ConfigReadableTests extends munit.FunSuite {
  test("Parse as integer") {
    val res = root.parse[ConfigNode]("a").flatMap(_.parse[Int]("b"))
    assert(res.contains(42))
  }
  test("Parse as string") {
    val res = root.parse[ConfigNode]("a").flatMap(_.parse[String]("b"))
    assert(res.contains("42"))
  }
  test("Type failure contains path") {
    val res = root.parse[ConfigNode]("a").flatMap(_.parse[Int]("c"))
    assert(res.isLeft)
    val err = res.left.toOption.get
    assert(err.path == NonEmptyList.of("a", "c"))
  }
  test("Missing key contains path") {
    val res = root.parse[ConfigNode]("a").flatMap(_.parse[String]("d"))
    assert(res.isLeft)
    val err = res.left.toOption.get
    assert(err.path == NonEmptyList.of("a", "d"))
  }
  test("Use dot-path") {
    val res = root.parse[String]("a.c")
    assert(res.isRight)
    assertEquals(res.toOption.get, "hello")
  }
  test("Error when using dot-path is reported correctly") {
    val res = root.parse[String]("a.d")
    assert(res.isLeft)
    val err = res.left.toOption.get
    assertEquals(err.path, NonEmptyList.of("a", "d"))
  }
  test("Parse object") {
    implicit val readC: ConfigReadable[C] = ConfigReadable.node.emap { node =>
      node.parse[String]("c").map(C.apply)
    }
    implicit val readAc: ConfigReadable[Ac] = ConfigReadable.node.emap { node =>
      node.parse[C]("a").map(Ac.apply)
    }
    val res = root.parse[C]("a")
  }

  def root = ConfigNode.root(testConf)
  def testConf = ConfigFactory.load("com/malliina/config/test.conf")
}

case class C(c: String)
case class Ac(a: C)
