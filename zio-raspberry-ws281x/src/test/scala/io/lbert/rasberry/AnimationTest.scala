package io.lbert.rasberry

import io.lbert.rasberry.GPIOModule.{GPIO, Pixel, PixelIndex}
import zio.console.Console
import zio.{Has, IO, UIO, ULayer, URLayer, ZLayer}
import zio.test._
import zio.test.mock._
import zio.test.mock.Expectation._
import zio.test.Assertion._
import zio.test.mock.MockConsole._

object GPIOMock {
  sealed trait Tag[I, A] extends Method[GPIO, I , A] {
    def envBuilder: URLayer[Has[Proxy], GPIO] =
      GPIOMock.envBuilder
  }

  final case object SetPixel extends Tag[Pixel, Unit]
  final case object Render extends Tag[Unit, Unit]
  final case object GetPixelCount extends Tag[Unit, Integer]

  private val envBuilder: URLayer[Has[Proxy], GPIO] =
    ZLayer.fromService(invoke =>
      new GPIO.Service {
        override def setPixel(pixel: Pixel): IO[GPIOModule.Error, Unit] = invoke(SetPixel, pixel)
        override def render(): IO[GPIOModule.Error, Unit] = invoke(Render)
        override def getPixelCount: UIO[Integer] = invoke(GetPixelCount)
      }
    )
}

object AnimationTest extends DefaultRunnableSpec {

  import GPIOMock._

  override def spec = suite("Animation Suite")(
    testM("calls GPIO correctly") {
      val pixel = Pixel(PixelIndex(0), Color(0, 0, 0))
      val app = Animation.setPixels(List(pixel))
      val mockEnv: ULayer[GPIO with Console] = (
        (PutStrLn(equalTo("Setting [1] pixels")) returns unit) andThen
        (SetPixel(equalTo(pixel)) returns unit) andThen
          (Render returns unit)
      )

      val result = app.provideLayer(mockEnv)
      assertM(result)(isUnit)
    }
  )
}
