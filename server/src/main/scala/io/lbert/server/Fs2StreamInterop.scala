package io.lbert.server

import cats.Functor
import zio.{Cause, Queue, RIO, Task, ZIO}
import zio.stream.{Take, ZStream}
import zio.interop.catz._
import cats.effect._
import cats.implicits._
import zio.interop.catz.implicits._

object Fs2StreamInterop {

  def toFs2[A](zstream: ZStream[Any, Throwable, A]): fs2.Stream[ZIO[Any, Throwable, *], A] =
    fs2.Stream.eval(fs2.concurrent.Queue.bounded[Task, A](1)).flatMap { fs2Queue =>
      fs2.Stream.eval[Task, Unit](
        zstream.mapM[Any, Throwable, Boolean](
          fs2Queue.offer1
        ).runDrain
      )
        .either(fs2Queue.dequeue)
        .collect { case Right(v) => v }
    }
}
