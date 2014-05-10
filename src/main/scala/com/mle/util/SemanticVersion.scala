package com.mle.util

/**
 * A partial implementation of http://semver.org/.
 *
 * @author Michael.
 */
case class SemanticVersion(version: String) {
  val (major, minor, patchString) = version.split(".", 3) match {
    case Array(ma, mi, pa) => (ma.toInt, mi.toInt, pa)
    case Array(ma, mi) => (ma.toInt, mi.toInt, "0")
    case Array(ma) => (ma.toInt, 0, "0")
    case other => (0, 0, "0")
  }
  val isPrerelease = patchString contains "-"
  val patch = patchInt(patchString)

  private def patchInt(p: String) = {
    val hyphenIndex = p indexOf "-"
    if (hyphenIndex == -1) p.toInt
    else p.substring(0, hyphenIndex).toInt
  }

  def ==(other: SemanticVersion) = version == other.version //major == other.major && minor == other.minor && patchString == other.patchString

  def !=(other: SemanticVersion) = !(this == other)

  def <(other: SemanticVersion) = major < other.major ||
    (major == other.major && minor < other.minor) ||
    (major == other.major && minor == other.minor && patch < other.patch) ||
    (major == other.major && minor == other.minor && patch == other.patch && isPrerelease && !other.isPrerelease)

  def <=(other: SemanticVersion) = this == other || this < other

  def >(other: SemanticVersion) = !(this < other) && this != other

  def >=(other: SemanticVersion) = this > other || this == other
}

object SemanticVersion {
  def parse(version: String): Either[NumberFormatException, SemanticVersion] =
    try {
      Right(SemanticVersion(version))
    } catch {
      case nfe: NumberFormatException => Left(nfe)
    }
}