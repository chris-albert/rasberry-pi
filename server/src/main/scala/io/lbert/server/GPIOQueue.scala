package io.lbert.server

import io.lbert.rasberry.GPIOModule.GPIO.Service
import io.lbert.rasberry.GPIOModule.{Error, GPIO, Pixel}
import zio.console.Console
import zio.stream.{Stream, ZStream}
import zio._

object GPIOQueue {

  type MessageStream  = Has[Stream[Nothing, Message]]
  type MessageStreamM = Has[UIO[Stream[Nothing, Message]]]

  sealed trait Message
  object Message {
    final case class SetPixel(pixel: Pixel) extends Message
    final case object Render extends Message
  }

  val live: ZLayer[Any, Nothing, GPIO with MessageStream] = ZLayer.fromEffectMany {
    Queue.bounded[Message](1).map(queue =>
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

  val broadcast: ZLayer[MessageStream with GPIO, Nothing, MessageStreamM with GPIO] = ZLayer.fromFunctionManyManaged { env =>
    val a = Has(env.get[GPIO.Service])

    val b = env.get[Stream[Nothing, Message]].broadcastDynamic(10)
    b.map(s => Has(s) ++ a)
  }

  val any: ZLayer[GPIO with MessageStream, Nothing, GPIO with MessageStream] =
    ZLayer.requires[GPIO with MessageStream]

  val consumeStream: ZIO[MessageStream with Console, Nothing, Unit] =
    for {
      stream <- ZIO.environment[MessageStream]
      _      <- stream.get.foreach(m => zio.console.putStrLn(s"Got message [$m]"))
    } yield ()

  val consumeStreamM: ZIO[MessageStreamM with Console, Nothing, Unit] =
    for {
      streamM <- ZIO.environment[MessageStreamM]
      stream  <- streamM.get
      _       <- stream.foreach(m => zio.console.putStrLn(s"Got message [$m]"))
    } yield ()
}
