package com.malliina.http

case class Person(name: String) derives KeyValues

enum Animal derives KeyValues:
  case Cat(mew: String)
  case Rhino(wroom: String, a: Int)

class QueryEncoderTests extends munit.FunSuite:
  test("Can do it"):
    val qe: KeyValues[Person] = KeyValues[Person]
    assertEquals(qe.kvs(Person("Jack")), List(KeyValue("name", "Jack")))

  test("enums to key values"):
    val a: Animal = Animal.Cat("Purr")
    val kve = KeyValues[Animal]
    assertEquals(kve.kvs(a), List(KeyValue("mew", "Purr")))
    val rhino = Animal.Rhino("Wum", 14)
    assertEquals(kve.kvs(rhino), List(KeyValue("wroom", "Wum"), KeyValue("a", "14")))
