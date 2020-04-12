package io.lbert.server

import io.lbert.rasberry.{Animation, Color, GPIOModule}
import io.lbert.rasberry.GPIOModule.GPIO
import zio.{Has, IO, ZLayer}
import zio.console.Console

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
    }

    val live: ZLayer[Console with GPIO, Nothing, LEDService] = ZLayer.fromFunction(env =>
      new Service {
        override def setAll(color: Color): IO[Error, Unit] =
          Animation.setAllPixelsToColor(color)
            .provide(env)
            .mapError(GPIOError)
      }
    )

    val any: ZLayer[LEDService, Nothing, LEDService] =
      ZLayer.requires[LEDService]
  }
}
