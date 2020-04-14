package io.lbert.server

import cats.Functor
import zio.{Cause, Queue, RIO, Task, ZIO}
import zio.stream.{Take, ZStream}
import zio.interop.catz._
import cats.effect._
import cats.implicits._
import zio.interop.catz.implicits._

object Fs2StreamInterop {

  def toFs2R[R, A](zstream: ZStream[Any, Throwable, A]): fs2.Stream[RIO[R, *], A] = ???

  def toFs2[A](zstream: ZStream[Any, Throwable, A]): fs2.Stream[Task, A] = {

    val out = fs2.Stream.eval(fs2.concurrent.Queue.bounded[Task, A](1)).flatMap { fs2Queue =>
      val b: ZStream[Any, Throwable, Boolean] = zstream.mapM{ a =>
        println(s"Offering [$a]")
        fs2Queue.offer1(a)
      }

      val c: Task[Unit] = b.runDrain

      fs2Queue.dequeue
    }

    out
  }


//  def toZStream[R, A](stream: fs2.Stream[RIO[R, *], A]): ZStream[R, Throwable, A] =
//    ZStream
//      .managed {
//        for {
//          queue <- Queue.bounded[Take[Throwable, A]](1).toManaged(_.shutdown)
//          _ <- ZIO
//            .runtime[R]
//            .toManaged_
//            .flatMap { implicit runtime =>
//              val b = implicitly[Functor[Task]]
//
//                ???
//            }
//            .fork
//        } yield ZStream.fromQueue(queue).unTake
//      }
//      .flatMap(identity)

//  def toZStream[R, A](stream: fs2.Stream[RIO[R, *], A]): ZStream[R, Throwable, A] =
//    ZStream
//      .managed {
//        for {
//          queue <- Queue.bounded[Take[Throwable, A]](1).toManaged(_.shutdown)
//          _ <- ZIO
//            .runtime[R]
//            .toManaged_
//            .flatMap { implicit runtime =>
//              val b = implicitly[Functor[Task]]
//              (stream.evalTap(a => queue.offer(Take.Value(a))) ++ fs2.Stream
//                .eval(queue.offer(Take.End)))
//                .handleErrorWith(e =>
//                  fs2.Stream.eval(queue.offer(Take.Fail(Cause.fail(e)))).drain)
//                .compile
//                .resource
//                .drain
//                .toManaged
//            }
//            .fork
//        } yield ZStream.fromQueue(queue).unTake
//      }
//      .flatMap(identity)
}
