package io.lbert.server

import cats.effect.ExitCode
import io.lbert.rasberry.GPIOModule.GPIO
import io.lbert.rasberry.GPIOStream.HasMessageStream
import io.lbert.rasberry.{GPIOModule, GPIOStream}
import io.lbert.server.LEDServiceModule.LEDService
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio._
import zio.logging.Logging

object Main extends App {

  def getServer: ZIO[zio.ZEnv with Has[API], Throwable, Unit] =
    for {
      api <- ZIO.environment[Has[API]]
      _   <- putStrLn("Listening on port [8080]")
      _   <- ZIO.runtime[zio.ZEnv].flatMap { implicit rts =>
        BlazeServerBuilder[Task]
          .bindHttp(
            8080,
            "0.0.0.0"
          )
          .enableHttp2(false)
          .withHttpApp(CORS(api.get.api.orNotFound))
          .serve
          .compile[Task, Task, ExitCode]
          .drain
      }
    } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val ledCount = 428

    val log = Logging.console((_, logEntry) => logEntry)

    val gpioStream = ZLayer.succeed(ledCount) >>> GPIOStream.live

    val ledService = (zio.ZEnv.any ++ gpioStream) >>> LEDService.live

    val ledStripGPIO = GPIOModule.stripLayer(ledsCount = ledCount) >>> GPIO.live

    val ledConsumer = GPIO.streamConsumer.provideSomeLayer[HasMessageStream](ledStripGPIO)

    val api = (zio.ZEnv.any ++ ledService ++ log ++ gpioStream) >>> API.live

    val prog = ledConsumer.fork *> getServer

    prog.provideLayer(zio.ZEnv.any ++ api ++ gpioStream).foldM(
      t => putStrLn(s"Error in program [$t]") *> IO.succeed(0),
      _ => putStrLn("Program exited successfully") *> IO.succeed(1)
    )
  }
}
