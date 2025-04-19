package com.malliina.http

import cats.Show

import scala.compiletime.{constValue, constValueTuple, erasedValue, summonInline}
import scala.deriving.Mirror

trait KeyValues[T]:
  def kvs(t: T): List[KeyValue]

object KeyValues:
  def apply[T](using qe: KeyValues[T]): KeyValues[T] = qe

  inline given derived[T](using m: Mirror.Of[T]): KeyValues[T] =
    inline m match
      case s: Mirror.SumOf[T] =>
        (x: T) =>
          val idx = s.ordinal(x)
          val instances = tcInstances[KeyValues, m.MirroredElemTypes]
          instances(idx).kvs(x)
      case p: Mirror.ProductOf[T] =>
        lazy val elemInstances = tcInstances[Show, m.MirroredElemTypes]
        val labels = constValueTuple[p.MirroredElemLabels].toList.asInstanceOf[List[String]]
        encProduct(p, elemInstances, labels)

  private def encProduct[T](
    p: Mirror.ProductOf[T],
    elemInstances: => List[Show[Any]],
    labels: List[String]
  ): KeyValues[T] =
    (x: T) =>
      val elems = x.asInstanceOf[Product].productIterator
      labels
        .zip(elems)
        .zip(elemInstances)
        .map:
          case ((label, value), enc) =>
            KeyValue(label, enc.show(value))

  private inline def tcInstances[TC[_], A <: Tuple]: List[TC[Any]] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        val headTypeClass = summonInline[TC[head]]
        val tailTypeClasses = tcInstances[TC, tail]
        headTypeClass.asInstanceOf[TC[Any]] :: tcInstances[TC, tail]
