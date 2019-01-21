package io.lbert.rasberry

import cats.effect.{Bracket, Sync}
import com.pi4j.io.gpio.{GpioController, GpioFactory, GpioPinDigitalOutput, PinState, Pin => JPin}
import cats.implicits._

trait GPIO[F[_]] {
  def togglePin(pin: JPin): F[Unit]
  def setPinState(pin: JPin, state: PinState): F[Unit]
}

object GPIO {

  def apply[F[_]](implicit G: GPIO[F]): GPIO[F] = G

  implicit def GPIOF[F[_]: Sync]: GPIO[F] = new GPIO[F] {

    override def togglePin(pin: JPin): F[Unit] = ???
//      get()

    override def setPinState(
      pin: JPin,
      state: PinState
    ): F[Unit] = ???
  }

  def get[F[_]: Sync, B](f: GpioController => F[B]): F[B] =
    Bracket[F, Throwable].bracket(
      Log[F].info("*** Creating GPIO instance ***") *>
        Sync[F].delay(GpioFactory.getInstance())
    )(f)(g =>
      Log[F].info("*** Shutting down GPIO instance ***") *>
        Sync[F].delay(g.shutdown())
    )
}

trait Pin[F[_]] {
  def output(
    pin: JPin,
    name: String = "Pin",
    state: PinState = PinState.HIGH
  ): F[String]
}

object Pin {
  def apply[F[_]](implicit P: Pin[F]): Pin[F] = P

  implicit def PinF[F[_]: Sync]: Pin[F] = ???
}

trait OutputPin[F[_]] {
  def toggle(): F[Unit]
}

object OutputPin {
  def apply[F[_]](implicit OP: OutputPin[F]): OutputPin[F] = OP

  implicit def OutputPinF[F[_]: Sync]: OutputPin[F] = ???
}