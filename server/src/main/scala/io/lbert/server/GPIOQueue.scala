package io.lbert.server

import io.lbert.rasberry.GPIOModule.GPIO.Service
import io.lbert.rasberry.GPIOModule.{Error, GPIO, Pixel}
import zio.console.Console
import zio.stream.{Stream, ZStream}
import zio._

object GPIOQueue {

  type MessageStream = Has[Stream[Nothing, Message]]

  sealed trait Message
  object Message {
    final case class SetPixel(pixel: Pixel) extends Message
    final case object Render extends Message
  }

  val live: ZLayer[Any, Nothing, GPIO with MessageStream] = ZLayer.fromEffectMany {
    Queue.sliding[Message](1000).map(queue =>
      Has(new Service {
        override def setPixel(pixel: Pixel): IO[Error, Unit] =
          queue.offer(Message.SetPixel(pixel)).unit

        override def render(): IO[Error, Unit] =
          queue.offer(Message.Render).unit

        override def getPixelCount: UIO[Integer] =
          IO.succeed(450)
      }) ++ Has(ZStream.fromQueue(queue))
    )
  }

  val any: ZLayer[GPIO with MessageStream, Nothing, GPIO with MessageStream] =
    ZLayer.requires[GPIO with MessageStream]

  val consumeStream: ZIO[MessageStream with Console, Nothing, Unit] =
    for {
      stream <- ZIO.environment[MessageStream]
      _      <- stream.get.foreach(m => zio.console.putStrLn(s"Got message [$m]"))
    } yield ()
}
