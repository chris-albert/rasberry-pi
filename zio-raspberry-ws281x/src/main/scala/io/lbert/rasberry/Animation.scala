package io.lbert.rasberry

import io.lbert.rasberry.GPIOModule.{Error, GPIO, Pixel, PixelIndex}
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration.Duration
import zio.stream.ZStream
import zio.{Schedule, ZIO}

object Animation {

  val allColors: List[(String, Color)] = Color.colorMapping

  def runThroughAllColorsStream(): ZStream[Console with GPIO, Error, Unit] =
    ZStream.fromIterable(allColors)
        .mapM {
          case (name, color) =>
            putStrLn(s"Setting all pixels to [$name]") *>
              setAllPixelsToColor(color)
        }

  def runThroughAllColors(duration: Duration): ZIO[Console with Clock with GPIO, Error, Unit] =
    runThroughAllColorsStream()
      .schedule(Schedule.spaced(duration)).runDrain

  def setAllPixelsToColor(color: Color): ZIO[Console with GPIO, Error, Unit] =
    for {
      _      <- putStrLn(s"Setting all pixels to [$color]")
      pixels <- foreachPixel(_ => color)
      _      <- setPixels(pixels)
    } yield ()

  def foreachPixel(f: PixelIndex => Color): ZIO[Console with GPIO, Error, List[Pixel]] =
    GPIO.getPixelCount.map(count =>
      (0 until count).map(i => Pixel(PixelIndex(i), f(PixelIndex(i)))).toList
    )

  def setPixels(pixels: List[Pixel]): ZIO[Console with GPIO, Error, Unit] =
    for {
      _     <- putStrLn(s"Setting [${pixels.size}] pixels")
//      _     <- ZIO.foreachPar(pixels)(GPIO.setPixel)
      _     <- ZIO.foreach(pixels)(GPIO.setPixel)
      _     <- GPIO.render()
    } yield ()
}
