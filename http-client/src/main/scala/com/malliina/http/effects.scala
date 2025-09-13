package com.malliina.http

import cats.MonadError
import cats.effect.Sync

import scala.concurrent.Future

trait Effects[F[_]] {
  def map[T, U](t: F[T])(f: T => U): F[U] = flatMap(t)(tv => success(f(tv)))
  def flatMap[T, U](t: F[T])(f: T => F[U]): F[U]
  def success[T](t: T): F[T]
  def fail[T](e: Exception): F[T]
}

class CatsEffects[F[_]: Sync](implicit F: MonadError[F, Throwable]) extends Effects[F] {
  override def flatMap[T, U](t: F[T])(f: T => F[U]): F[U] = F.flatMap(t)(f)
  override def success[T](t: T): F[T] = Sync[F].blocking(t)
  override def fail[T](e: Exception): F[T] = F.raiseError(e)
}

trait FutureEffects extends Effects[Future] {}
