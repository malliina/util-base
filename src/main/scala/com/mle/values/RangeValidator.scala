package com.mle.values

import play.api.libs.json._

/**
 * @author Michael
 */
trait RangeValidator[T, U] extends ValueValidator[T, U] {
  def empty = build(Default)

  lazy val MinValue = build(Min)

  lazy val MaxValue = build(Max)

  def Default: T = Min

  def Min: T

  def Max: T

  def jsonFormat(implicit format: Format[T]) = new Format[U] {
    override def reads(json: JsValue): JsResult[U] =
      json.validate[T].flatMap(i => from(i)
        .map(c => JsSuccess(c))
        .getOrElse(JsError(s"Value out of range: $i, must be within: [$Min, $Max]")))

    override def writes(o: U): JsValue = Json.toJson(strip(o))
  }
}
