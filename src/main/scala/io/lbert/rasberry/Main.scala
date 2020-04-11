package io.lbert.rasberry

import java.util.concurrent.TimeUnit
import _root_.zio.{App, IO, ZIO}
import io.lbert.rasberry.GPIOModule.GPIO
import zio.console.putStrLn
import zio.duration.Duration

object Main extends App {

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {

    val layer = (zio.ZEnv.any ++ GPIOModule.stripLayer()) >>> GPIO.live
//    val layer = (zio.ZEnv.any) >>> GPIO.fake

    val animation = Animation.runThroughAllColors(Duration(5, TimeUnit.SECONDS))
      .provideSomeLayer(layer)

    animation.foldM(
      t => putStrLn(s"Finished with error [$t]") *> IO.succeed(1),
      _ => putStrLn("Finished successfully") *> IO.succeed(0)
    )
  }
}
