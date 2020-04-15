package io.lbert.rasberry

import com.github.mbelling.ws281x.{LedStripType, Ws281xLedStrip}
import zio.stream.Stream
import zio._

object GPIOModule {

  type GPIO = Has[GPIO.Service]

  sealed trait Error
  object Error {
    final case class StripError(error: Throwable) extends Error
  }
  import Error._

  object GPIO {

    trait Service {
      def setPixel(pixel: Pixel): IO[Error, Unit]
      def render(): IO[Error, Unit]
      def getPixelCount: UIO[Integer]
    }

    val live: ZLayer[Has[Ws281xLedStrip], Nothing, GPIO] = ZLayer.fromFunction(env =>
      new Service {
        override def setPixel(pixel: Pixel): IO[Error, Unit] =
          ZIO.effect(env.get.setPixel(pixel.index.index, Color.toLEDColor(pixel.color)))
          .mapError(StripError)

        override def render(): IO[Error, Unit] =
          ZIO.effect(env.get.render()).mapError(StripError)

        override def getPixelCount: UIO[Integer] =
          ZIO.effectTotal(env.get.getLedsCount)
      }
    )

//    val queueConsumer: ZIO[]

    def setPixel(pixel: Pixel): ZIO[GPIO, Error, Unit] =
      ZIO.accessM(_.get.setPixel(pixel))

    def render(): ZIO[GPIO, Error, Unit] =
      ZIO.accessM(_.get.render())

    def getPixelCount: ZIO[GPIO, Error, Integer] =
      ZIO.accessM(_.get.getPixelCount)
  }

  def stripLayer(
    ledsCount  : Int = 450,
    gpioPin    : Int = 18,
    frequencyHz: Int = 800000,
    dma        : Int = 10,
    brightness : Int = 255,
    channel    : Int = 0,
    invert     : Boolean = false,
    stripType  : LedStripType = LedStripType.WS2811_STRIP_GRB,
    cleanOnExit: Boolean = true
  ): ZLayer[Any, Nothing, Has[Ws281xLedStrip]] = ZLayer.fromEffect(getStrip(
    ledsCount,
    gpioPin,
    frequencyHz,
    dma,
    brightness,
    channel,
    invert,
    stripType,
    cleanOnExit
  ))

  def getStrip(
    ledsCount  : Int = 450,
    gpioPin    : Int = 18,
    frequencyHz: Int = 800000,
    dma        : Int = 10,
    brightness : Int = 255,
    channel    : Int = 0,
    invert     : Boolean = false,
    stripType  : LedStripType = LedStripType.WS2811_STRIP_GRB,
    cleanOnExit: Boolean = true
  ): UIO[Ws281xLedStrip] = UIO.effectTotal(new Ws281xLedStrip(ledsCount, gpioPin,
    frequencyHz, dma, brightness, channel, invert, stripType, cleanOnExit))

  final case class PixelIndex(index: Integer)

  final case class Pixel(index: PixelIndex, color: Color)

}
