package io.lbert.server

import io.lbert.rasberry.{Animation, Color, GPIOModule}
import io.lbert.rasberry.GPIOModule.{Brightness, GPIO, Pixel}
import zio.clock.Clock
import zio.{Has, IO, ZLayer}
import zio.logging.Logging.Logging

object LEDServiceModule {

  type LEDService = Has[LEDService.Service]

  sealed trait Error
  object Error {
    final case class GPIOError(error: GPIOModule.Error) extends Error
  }
  import Error._

  object LEDService {

    trait Service {
      def setAll(color: Color): IO[Error, Unit]
      def set(pixels: List[Pixel]): IO[Error, Unit]
      def animate(animation: Animation): IO[Error, Unit]
      def setBrightness(brightness: Brightness): IO[Error, Unit]
    }

    val live: ZLayer[Logging with GPIO with Clock, Nothing, LEDService] = ZLayer.fromFunction(env =>
      new Service {
        override def setAll(color: Color): IO[Error, Unit] =
          Animation.setAllPixelsToColor(color)
            .provide(env)
            .mapError(GPIOError)

        override def setBrightness(brightness: Brightness): IO[Error, Unit] =
          Animation.setBrightness(brightness)
            .provide(env)
            .mapError(GPIOError)

        override def set(pixels: List[Pixel]): IO[Error, Unit] =
          Animation.setPixels(pixels)
          .provide(env)
          .mapError(GPIOError)

        override def animate(animation: Animation): IO[Error, Unit] =
          Animation.animate(animation)
            .provide(env)
            .mapError(GPIOError)
      }
    )

    val any: ZLayer[LEDService, Nothing, LEDService] =
      ZLayer.requires[LEDService]
  }
}
