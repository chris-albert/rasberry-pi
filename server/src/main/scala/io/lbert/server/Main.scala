package io.lbert.server

import cats.effect.ExitCode
import io.lbert.rasberry.GPIOModule.GPIO
import io.lbert.rasberry.{GPIOModule, GPIOStream}
import io.lbert.server.LEDServiceModule.LEDService
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio._
import zio.logging.slf4j.Slf4jLogger

object Main extends App {

  private val ledCount = 428

  private val log = Slf4jLogger.make((_, message) => message)

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

  private def runStream(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val gpio = (ZLayer.succeed(ledCount) ++ log) >>> GPIO.fake

    val ledService = (zio.ZEnv.any ++ gpio ++ log) >>> LEDService.live

//    val ledStripGPIO = GPIOModule.stripLayer(ledsCount = ledCount) >>> GPIO.live
//    val ledStripGPIO = (ZLayer.succeed(ledCount) ++ log) >>> GPIO.fake

    val api = (zio.ZEnv.any ++ ledService ++ log ++ GPIOStream.deadStream) >>> API.live

    val prog = getServer

    prog.provideLayer(zio.ZEnv.any ++ api).foldM(
      t => putStrLn(s"Error in program [$t]") *> IO.succeed(0),
      _ => putStrLn("Program exited successfully") *> IO.succeed(1)
    )
  }

  private def runDirect(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val gpioStream = GPIOStream.deadStream

    val ledStripGPIO = (log ++ GPIOModule.stripLayer(ledsCount = ledCount)) >>> GPIO.live

    val ledService = (zio.ZEnv.any ++ gpioStream ++ ledStripGPIO ++ log) >>> LEDService.live

    val api = (zio.ZEnv.any ++ ledService ++ log ++ gpioStream) >>> API.live

    val prog = getServer

    prog.provideLayer(zio.ZEnv.any ++ api ++ gpioStream).foldM(
      t => putStrLn(s"Error in program [$t]") *> IO.succeed(0),
      _ => putStrLn("Program exited successfully") *> IO.succeed(1)
    )
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    if(true) runDirect(args) else runStream(args)
}
