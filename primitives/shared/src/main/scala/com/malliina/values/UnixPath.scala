package com.malliina.values

import java.nio.file.Path

/** @param path
  *   a path - all separators must be slashes ('/') regardless of platform
  */
case class UnixPath(path: String) extends WrappedString {
  def value: String = path
}

object UnixPath extends StringCompanion[UnixPath] {
  val UnixPathSeparator: Char = '/'
  val WindowsPathSeparator = '\\'
  val Empty: UnixPath = UnixPath("")

  override def build(input: String): Either[ErrorMessage, UnixPath] = Right(fromRaw(input))

  def apply(path: Path): UnixPath = fromRaw(path.toString)

  def fromRaw(s: String): UnixPath = UnixPath(s.replace(WindowsPathSeparator, UnixPathSeparator))
}
