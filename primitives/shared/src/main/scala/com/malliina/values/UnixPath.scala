package com.malliina.values

import java.nio.file.Path

/**
  * @param path a path - all separators must be slashes ('/') regardless of platform
  */
case class UnixPath(path: String) extends Wrapped(path)

object UnixPath extends StringCompanion[UnixPath] {
  val UnixPathSeparator: Char = '/'
  val WindowsPathSeparator = '\\'
  val Empty = UnixPath("")

  def apply(path: Path): UnixPath = fromRaw(path.toString)

  def fromRaw(s: String): UnixPath = UnixPath(s.replace(WindowsPathSeparator, UnixPathSeparator))
}
