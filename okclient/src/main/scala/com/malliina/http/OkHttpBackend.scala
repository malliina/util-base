package com.malliina.http

import java.io.Closeable

import okhttp3.OkHttpClient

trait OkHttpBackend extends Closeable {
  def client: OkHttpClient

  override def close(): Unit = {
    client.dispatcher().executorService().shutdown()
    client.connectionPool().evictAll()
    Option(client.cache()).foreach(_.close())
  }
}
