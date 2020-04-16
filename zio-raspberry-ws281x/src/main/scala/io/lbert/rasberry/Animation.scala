package io.lbert.rasberry

import io.lbert.rasberry.GPIOModule.{Brightness, Error, GPIO, Pixel, PixelIndex}
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.duration.Duration
import zio.stream.ZStream
import zio.{Schedule, ZIO}

sealed trait Animation

object Animation {

  final case class Sequence(duration: Duration) extends Animation
  final case class Wipe(duration: Duration) extends Animation
  final case class TheaterChase(duration: Duration, color: Color, channels: Int) extends Animation

  def animate(animation: Animation): ZIO[Console with Clock with GPIO, Error, Unit] =
    animation match {
      case Sequence(duration)     => runThroughAllColors(duration)
      case Wipe(duration)         => wipeStream(duration).runDrain
      case TheaterChase(d, c, ch) => theaterChase(d, c, ch)
    }

  val allColors: List[(String, Color)] = List(
    "white"      -> Color(255, 255, 255),
    "pink"       -> Color(255, 175, 175),
    "red"        -> Color(255, 0, 0),
    "orange"     -> Color(255, 200, 0),
    "yellow"     -> Color(255, 255, 0),
    "green"      -> Color(0, 255, 0),
    "magenta"    -> Color(255, 0, 255),
    "cyan"       -> Color(0, 255, 255),
    "blue"       -> Color(0, 0, 255)
  )

  def wheel(pos: Int): Color =
    if(pos < 85) Color(pos * 3, 255 - pos * 3, 0)
    else if(pos < 170) Color(255 - (pos - 85) * 3, 0, (pos - 85) * 3)
    else Color(0, (pos - 170) * 3, 255 - (pos - 170) * 3)

  def getTheaterChase(duration: Duration, color: Color, channels: Int): ZStream[Console with Clock with GPIO, Error, List[Pixel]] = {
    val each = ZIO.foreach(0 until channels)(channel =>
      foreachPixel(i => if((i.index + channel) % channels == 0) color else Color.Black)
    )
    ZStream.fromIterableM(each)
      .forever
  }

  def theaterChase(duration: Duration, color: Color, channels: Int): ZIO[Console with Clock with GPIO, Error, Unit] = {
    getTheaterChase(duration, color, channels)
        .mapM(setPixels)
        .schedule(Schedule.spaced(duration))
        .runDrain
  }

  def wipeStream(duration: Duration): ZStream[Console with Clock with GPIO, Error, Unit] = {
    ZStream.fromIterable(allColors)
      .mapM {
        case (_, color) =>
          foreachPixel(_ => color).flatMap(pixels =>
            ZStream.fromIterable(pixels)
              .mapM(p => setPixels(List(p)))
              .schedule(Schedule.spaced(duration))
              .runDrain
          )
      }
  }

  def runThroughAllColorsStream(): ZStream[Console with GPIO, Error, Unit] =
    ZStream.fromIterable(allColors)
        .mapM {
          case (name, color) =>
            putStrLn(s"Setting all pixels to [$name]") *>
              setAllPixelsToColor(color)
        }

  def runThroughAllColors(duration: Duration): ZIO[Console with Clock with GPIO, Error, Unit] =
    runThroughAllColorsStream().forever
      .schedule(Schedule.spaced(duration)).runDrain

  def setAllPixelsToColor(color: Color): ZIO[Console with GPIO, Error, Unit] =
    for {
      _      <- putStrLn(s"Setting all pixels to [$color]")
      pixels <- foreachPixel(_ => color)
      _      <- setPixels(pixels)
    } yield ()

  def setBrightness(brightness: Brightness): ZIO[Console with GPIO, Error, Unit] =
    GPIO.setBrightness(brightness) *> GPIO.render()

  def foreachPixel(f: PixelIndex => Color): ZIO[Console with GPIO, Error, List[Pixel]] =
    GPIO.getPixelCount.map(count =>
      (0 until count).map(i => Pixel(PixelIndex(i), f(PixelIndex(i)))).toList
    )

  def setPixels(pixels: List[Pixel]): ZIO[Console with GPIO, Error, Unit] =
    for {
      _     <- putStrLn(s"Setting [${pixels.size}] pixels")
      _     <- ZIO.foreachPar(pixels)(GPIO.setPixel)
      _     <- GPIO.render()
    } yield ()
}
