package io.lbert.server

import zio.{Task, ZIO}
import zio.stream.ZStream
import zio.interop.catz._

object Fs2StreamInterop {

  def toFs2[A](zstream: ZStream[Any, Throwable, A]): fs2.Stream[ZIO[Any, Throwable, *], A] =
    fs2.Stream.eval(fs2.concurrent.Queue.bounded[Task, A](1000)).flatMap { fs2Queue =>
      fs2.Stream.eval[Task, Unit](
        zstream.mapM[Any, Throwable, Boolean](
          fs2Queue.offer1
        ).runDrain
      )
        .either(fs2Queue.dequeue)
        .collect { case Right(v) => v }
    }
}
