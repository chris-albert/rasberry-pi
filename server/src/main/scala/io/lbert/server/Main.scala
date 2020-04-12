package io.lbert.server

import cats.effect.ExitCode
import io.lbert.rasberry.GPIOModule
import io.lbert.rasberry.GPIOModule.GPIO
import io.lbert.server.LEDServiceModule.LEDService
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.{App, Has, IO, Task, ZIO, ZLayer}
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.implicits._
import zio.logging.LogAnnotation
import zio.logging.slf4j.Slf4jLogger

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
          .enableHttp2(true)
          .withHttpApp(CORS(api.get.api.orNotFound))
          .serve
          .compile[Task, Task, ExitCode]
          .drain
      }
    } yield ()

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {


    val log = ZLayer.fromEffect(Slf4jLogger.make{(_, message) => message})

    val gpio = GPIOModule.stripLayer() >>> GPIO.live

    val led = (zio.ZEnv.any ++ gpio) >>> LEDService.live

    val api = (zio.ZEnv.any ++ led ++ log) >>> API.live

    val prog = getServer
      .provideSomeLayer(zio.ZEnv.any ++ api)

    prog.foldM(
      t => putStrLn(s"Error in program [$t]") *> IO.succeed(0),
      _ => putStrLn("Program exited successfully") *> IO.succeed(1)
    )
  }
}
