package io.lbert.rasberry

import cats.effect.{ExitCode, IO, IOApp}
import com.pi4j.io.gpio._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    impure(args)

  def impure(args: List[String]): IO[ExitCode] = {
    println("Staring pin cycle test")
    val gpio = GpioFactory.getInstance()

    val pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "MyLED", PinState.HIGH)

    pin.setShutdownOptions(true, PinState.LOW)

    pin.low()
    println("Starting pin cycle")
    while(true) {
      println("Toggle")
      pin.toggle()
      Thread.sleep(2000)
    }
    gpio.shutdown()

    println("Shutting down")
    IO.pure(ExitCode.Success)
  }



}
