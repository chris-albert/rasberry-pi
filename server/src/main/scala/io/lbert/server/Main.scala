package io.lbert.server

import cats.effect.ExitCode
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.{App, Has, IO, Task, ZIO}
import zio.console._
import zio.interop.catz._
import zio.interop.catz.implicits._
import org.http4s.implicits._

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

    val prog = getServer
      .provideSomeLayer(zio.ZEnv.any ++ API.live)

    prog.foldM(
      t => putStrLn(s"Error in program [$t]") *> IO.succeed(0),
      _ => putStrLn("Program exited successfully") *> IO.succeed(1)
    )
  }
}
