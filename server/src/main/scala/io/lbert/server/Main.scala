package io.lbert.server

import cats.effect.ExitCode
import io.lbert.server.LEDServiceModule.LEDService
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.logging.slf4j.Slf4jLogger
import zio._

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

    val log = Slf4jLogger.make{(_, message) => message}

    val gpioQueue = GPIOQueue.live
    val gpio = gpioQueue

    val led = (zio.ZEnv.any ++ gpio) >>> LEDService.live

    val api = (zio.ZEnv.any ++ led ++ log ++ gpio) >>> API.live

    val prog = GPIOQueue.consumeStream.fork *> getServer

    prog.provideLayer(zio.ZEnv.any ++ api ++ gpio).foldM(
      t => putStrLn(s"Error in program [$t]") *> IO.succeed(0),
      _ => putStrLn("Program exited successfully") *> IO.succeed(1)
    )
  }
}
