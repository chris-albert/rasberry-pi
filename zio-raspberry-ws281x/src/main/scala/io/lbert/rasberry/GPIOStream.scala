package io.lbert.rasberry

import io.lbert.rasberry.GPIOModule.GPIO.Service
import io.lbert.rasberry.GPIOModule.{Brightness, Error, GPIO, Pixel}
import zio.console.Console
import zio.stream.{Stream, ZStream}
import zio._

object GPIOStream {

  type HasMessageStream = Has[MessageStream]
  type MessageStream = UIO[Stream[Nothing, Message]]

  sealed trait Message
  object Message {
    final case class SetPixel(pixel: Pixel) extends Message
    final case class SetBrightness(brightness: Brightness) extends Message
    final case object Render extends Message
  }

  val live: ZLayer[Has[Int], Nothing, GPIO with HasMessageStream] = ZLayer.fromFunctionManyManaged { ledCount =>
    Queue.bounded[Message](1).toManaged(_.shutdown)
        .flatMap(queue =>
          ZStream.fromQueue(queue).broadcastDynamic(10).map(broadcastStream =>
            Has(new Service {
              override def setPixel(pixel: Pixel): IO[Error, Unit] =
                queue.offer(Message.SetPixel(pixel)).unit

              override def render(): IO[Error, Unit] =
                queue.offer(Message.Render).unit

              override def setBrightness(brightness: Brightness): IO[Error, Unit] =
                queue.offer(Message.SetBrightness(brightness)).unit

              override def getPixelCount: UIO[Int] =
                IO.succeed(ledCount.get)
            }) ++ Has(broadcastStream)
          )
        )
  }

  val any: ZLayer[GPIO with HasMessageStream, Nothing, GPIO with HasMessageStream] =
    ZLayer.requires[GPIO with HasMessageStream]

  val printMessageStream: ZIO[HasMessageStream with Console, Nothing, Unit] =
    for {
      streamM <- ZIO.environment[HasMessageStream]
      stream  <- streamM.get
      _       <- stream.foreach(m => zio.console.putStrLn(s"Got message [$m]"))
    } yield ()
}
