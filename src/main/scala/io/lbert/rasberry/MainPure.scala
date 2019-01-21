package io.lbert.rasberry


import cats.effect.{ExitCode, IO, IOApp, Sync}
import com.pi4j.io.gpio.{Pin => JPin, _}
import cats.implicits._
import fs2.Stream

object MainPure extends IOApp{
  override def run(args: List[String]): IO[ExitCode] =
    pure()

  def pure(): IO[ExitCode] = for {
    _ <- togglePinForever[IO](RaspiPin.GPIO_01)
  } yield ExitCode.Success

  def togglePinForever[F[_]: Sync](pin: JPin): F[Unit] = {
    Stream.eval(GPIO[F].togglePin(pin)).compile.drain
  }
}
