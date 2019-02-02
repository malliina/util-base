package com.malliina.json

import com.malliina.values.StringEnumCompanion

@deprecated("Use StringEnumCompanion", "1.8.0")
abstract class JsonEnumSet[T] extends StringEnumCompanion[T] {
  def withName(name: String): Option[T] =
    all.find(i => resolveName(i).toLowerCase == name.toLowerCase)
}
