package com.malliina.util

import java.io.Closeable

import scala.concurrent.duration._
import scala.language.reflectiveCalls

object Utils {
  val suppresser: PartialFunction[Throwable, Unit] = {
    case _: Throwable => ()
  }

  def runnable(f: => Any): Runnable = new Runnable {
    def run(): Unit = f
  }

  def using[T <: Closeable, U](resource: T)(op: T => U): U =
    try {
      op(resource)
    } finally {
      resource.close()
    }

  /** Performs the given operation on the provided closeable resource after which the resource is closed.
    *
    * @see [[com.malliina.util.Utils]].using
    * @param resource the resource to operate on: a file reader, database connection, ...
    * @param op       the operation to perform on the resource: read/write to a file, database, ...
    * @tparam T closeable resource
    * @tparam U result of the operation
    * @return the result of the operation
    */
  def resource[T <: {def close()}, U](resource: T)(op: T => U): U =
    try {
      op(resource)
    } finally {
      resource.close()
    }


  def optionally[T, U <: Throwable](attempt: => T)(implicit manifest: Manifest[U]): Either[U, T] =
    try {
      Right(attempt)
    } catch {
      case u: U => Left(u)
    }

  /** Attempts to compute `attempt`, suppressing the specified exception.
    *
    * @return attempt wrapped in an [[scala.Option]], or [[scala.None]] if an exception of type U is thrown
    */
  def opt[T, U <: Throwable](attempt: => T)(implicit manifest: Manifest[U]): Option[T] =
    optionally(attempt).toOption

  def timed[T](f: => T): (T, Duration) = {
    val start = System.currentTimeMillis()
    val result = f
    val end = System.currentTimeMillis()
    (result, (end - start).millis)
  }

}
