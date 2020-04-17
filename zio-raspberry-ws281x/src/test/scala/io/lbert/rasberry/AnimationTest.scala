package io.lbert.rasberry

import io.lbert.rasberry.GPIOModule.{GPIO, Pixel, PixelIndex}
import zio.console.Console
import zio.logging.Logging
import zio.logging.Logging.Logging
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
  final case object GetPixelCount extends Tag[Unit, Int]

  private val envBuilder: URLayer[Has[Proxy], GPIO] =
    ZLayer.fromService(invoke =>
      new GPIO.Service {
        override def setPixel(pixel: Pixel): IO[GPIOModule.Error, Unit] = invoke(SetPixel, pixel)
        override def render(): IO[GPIOModule.Error, Unit] = invoke(Render)
        override def getPixelCount: UIO[Int] = invoke(GetPixelCount)
        override def setBrightness(brightness: GPIOModule.Brightness): IO[GPIOModule.Error, Unit] = ???
      }
    )
}

object AnimationTest extends DefaultRunnableSpec {

  import GPIOMock._

  override def spec = suite("Animation Suite")(
    testM("calls GPIO correctly") {
      val pixel = Pixel(PixelIndex(0), Color(0, 0, 0))
      val app = Animation.setPixels(List(pixel))
      val mockEnv: ULayer[GPIO] = (
        (SetPixel(equalTo(pixel)) returns unit) andThen
          (Render returns unit)
      )

      val result = app.provideLayer(mockEnv ++ Logging.console((_, m) => m))
      assertM(result)(isUnit)
    },
    test("getTheaterChase not flipped") {
      val a = Animation.getTheaterChaseInitial(3, false, 1)
      assert(a)(equalTo(List(
        List(true, false, false),
        List(false, true, false),
        List(false, false, true)
      )))
    },
    test("getTheaterChase flipped") {
      val a = Animation.getTheaterChaseInitial(3, true, 1)
      assert(a)(equalTo(List(
        List(true, false, false),
        List(false, false, true),
        List(false, true, false)
      )))
    }
  )
}
