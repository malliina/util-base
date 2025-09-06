package com.malliina.http

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.util.zip.{GZIPInputStream, InflaterInputStream}

trait Decompressor {
  def decompress(compressed: Array[Byte]): Array[Byte]
}

object Decompression {
  val gzip = Decompression.of(in => new GZIPInputStream(in))
  val deflate = Decompression.of(in => new InflaterInputStream(in))
  val identity = new Decompressor {
    override def decompress(compressed: Array[Byte]): Array[Byte] = compressed
  }

  def of(pipe: InputStream => InputStream): Decompressor = bytes =>
    using(new ByteArrayOutputStream()) { os =>
      using(pipe(new ByteArrayInputStream(bytes))) { gzIn =>
        gzIn.transferTo(os)
      }
      os.toByteArray
    }

  def using[T <: AutoCloseable, U](res: T)(code: T => U): U =
    try code(res)
    finally res.close()
}
