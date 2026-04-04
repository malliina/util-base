package com.malliina.util

object Randoms extends Randoms("abcdefghijklmnopqrstuvwxyz0123456789")

class Randoms(chars: String) {
  def randomString(length: Int): String =
    (1 to length).map(_ => randomChar()).mkString

  def randomChar(): Char =
    chars.charAt(math.floor(math.random() * chars.length).toInt)
}
