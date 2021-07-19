package com.malliina.storage

import io.circe._

class StorageSize(bytes: Long) {
  private val k = 1024

  def toBytes = bytes
  def toKilos = bytes / k
  def toKilosDouble = 1.0d * bytes / k
  def toMegs = toKilos / k
  def toMegsDouble = toKilosDouble / k
  def toGigs = toMegs / k
  def toGigsDouble = toMegsDouble / k
  def toTeras = toGigs / k
  def toTerasDouble = toGigsDouble / k

  def <(other: StorageSize) = toBytes < other.toBytes
  def <=(other: StorageSize) = toBytes <= other.toBytes
  def >(other: StorageSize) = toBytes > other.toBytes
  def >=(other: StorageSize) = this.toBytes >= other.toBytes
  def ==(other: StorageSize) = this.toBytes == other.toBytes
  def !=(other: StorageSize) = this.toBytes != other.toBytes

  /**
    * @return a string of format 'n units'
    */
  override def toString: String =
    if (toTeras > 10) s"$toTeras terabytes"
    else if (toGigs > 10) s"$toGigs gigabytes"
    else if (toMegs > 10) s"$toMegs megabytes"
    else if (toKilos > 10) s"$toKilos kilobytes"
    else s"$toBytes bytes"
}

object StorageSize {
  val empty = new StorageSize(0)

  implicit val json: Codec[StorageSize] = Codec.from(
    Decoder.decodeLong.map(_.bytes),
    Encoder.encodeLong.contramap[StorageSize](_.toBytes)
  )
}
