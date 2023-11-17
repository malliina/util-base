package com.malliina

/** Adapted from concurrent.duration._
  *
  * Enables: 5.megs, 6.gigs etc.
  */
package object storage {
  val k: Long = 1024L

  /** @param amount
    *   integer amount of some storage unit
    */
  implicit final class StorageInt(val amount: Int) extends StorageConversions {
    protected def asBytes(multiplier: Long): Long = multiplier * amount
  }

  implicit final class StorageLong(val amount: Long) extends StorageConversions {
    protected def asBytes(multiplier: Long): Long = multiplier * amount
  }

  implicit final class StorageDouble(val amount: Double) extends StorageConversions {
    protected def asBytes(multiplier: Long): Long = (multiplier * amount).toLong
  }

  trait StorageConversions {
    protected def asStorageSize(multiplier: Long): StorageSize =
      StorageSize(asBytes(multiplier))

    protected def asBytes(multiplier: Long): Long

    def bytes = asStorageSize(1)
    def kilos = asStorageSize(k)
    def megs = asStorageSize(k * k)
    def gigs = asStorageSize(k * k * k)
    def teras = asStorageSize(k * k * k * k)
  }
}
