package com.malliina.util

import java.io.{OutputStream, InputStream}

trait Streams {
  // http://stackoverflow.com/questions/6927873/how-can-i-read-a-file-to-an-inputstream-then-write-it-into-an-outputstream-in-sc
  def stream(inputStream: InputStream, outputStream: OutputStream, bufferSize: Int = 16384): Long = {
    val buffer = new Array[Byte](bufferSize)

    def doStream(total: Long = 0): Long = {
      val n = inputStream.read(buffer)
      if (n == -1) {
        total
      } else {
        outputStream.write(buffer, 0, n)
        doStream(total + n)
      }
    }

    doStream()
  }
}

object Streams extends Streams
