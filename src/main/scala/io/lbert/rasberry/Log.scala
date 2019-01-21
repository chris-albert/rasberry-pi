package io.lbert.rasberry

import cats.effect.Sync

trait Log[F[_]] {
  def info(s: String): F[Unit]
}

object Log {
  def apply[F[_]](implicit L: Log[F]): Log[F] = L

  implicit def LoggerF[F[_]: Sync]: Log[F] = new Log[F] {
    override def info(s: String): F[Unit] =
      Sync[F].delay(println(s))
  }
}

