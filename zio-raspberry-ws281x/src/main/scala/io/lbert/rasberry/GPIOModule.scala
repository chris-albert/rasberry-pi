package io.lbert.rasberry

import com.github.mbelling.ws281x.{LedStripType, Ws281xLedStrip}
import io.lbert.rasberry.GPIOStream.{HasMessageStream, Message}
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
      def getPixelCount: UIO[Int]
      def setBrightness(brightness: Brightness): IO[Error, Unit]
    }

    val live: ZLayer[Has[Ws281xLedStrip], Nothing, GPIO] = ZLayer.fromFunction(env =>
      new Service {
        override def setPixel(pixel: Pixel): IO[Error, Unit] =
          ZIO.effect(env.get.setPixel(pixel.index.index, Color.toLEDColor(pixel.color)))
          .mapError(StripError)

        override def render(): IO[Error, Unit] =
          ZIO.effect(env.get.render()).mapError(StripError)

        override def setBrightness(brightness: Brightness): IO[Error, Unit] =
          ZIO.effect(env.get.setBrightness(brightness.brightness))
            .mapError(StripError)

        override def getPixelCount: UIO[Int] =
          ZIO.effectTotal(env.get.getLedsCount)
      }
    )

    val streamConsumer: ZIO[HasMessageStream with GPIO, Error, Unit] =
      for {
        streamM <- ZIO.environment[HasMessageStream]
        stream  <- streamM.get
        _       <- stream.foreach {
          case Message.SetPixel(pixel)           => GPIO.setPixel(pixel)
          case Message.SetBrightness(brightness) => GPIO.setBrightness(brightness)
          case Message.Render                    => GPIO.render()
        }
      } yield ()

    def setPixel(pixel: Pixel): ZIO[GPIO, Error, Unit] =
      ZIO.accessM(_.get.setPixel(pixel))

    def setBrightness(brightness: Brightness): ZIO[GPIO, Error, Unit] =
      ZIO.accessM(_.get.setBrightness(brightness))

    def render(): ZIO[GPIO, Error, Unit] =
      ZIO.accessM(_.get.render())

    def getPixelCount: ZIO[GPIO, Error, Int] =
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

  final case class PixelIndex(index: Int)

  final case class Pixel(index: PixelIndex, color: Color)

  final case class Brightness(brightness: Int)

}
