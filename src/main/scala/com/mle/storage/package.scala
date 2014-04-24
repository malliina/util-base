package com.mle

/**
 * Adapted from concurrent.duration._
 *
 * Enables: 5.megs, 6.gigs etc.
 *
 * @author mle
 */
package object storage {
  private val k = 1024L

  /**
   * @param amount integer amount of some storage unit
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
    protected def asStorageSize(multiplier: Long): StorageSize = new StorageSize(asBytes(multiplier))

    protected def asBytes(multiplier: Long): Long

    def bytes = asStorageSize(1)

    def kilos = asStorageSize(k)

    def megs = asStorageSize(k * k)

    def gigs = asStorageSize(k * k * k)

    def teras = asStorageSize(k * k * k * k)
  }

}


