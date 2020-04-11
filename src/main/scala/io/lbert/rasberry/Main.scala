package io.lbert.rasberry

import cats.effect.{ExitCode, IO, IOApp}
import com.github.mbelling.ws281x.{Color, LedStripType, Ws281xLedStrip}
import com.pi4j.io.gpio._


object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    runStrip()

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

  def colorWipe(strip: Ws281xLedStrip, color: Color): Unit = {
    forAllLeds(strip){_ =>
//      Thread.sleep(50)
      color
    }
  }

  def forAllLeds(strip: Ws281xLedStrip)(f: Int => Color): Unit = {
    (0 until strip.getLedsCount).foreach{i =>
      val color = f(i)
      strip.setPixel(i, color)
    }
    strip.render()
  }

  def runStrip(): IO[ExitCode] = {

    val strip = getStrip()

    allColors.foreach{ case(name, color) =>
      println(s"Rendering color [$name]")
      colorWipe(strip, color)
      Thread.sleep(5000)
    }
    println("End")
    IO.pure(ExitCode.Success)
  }

  val allColors: Seq[(String,Color)] = Seq(
    "white"   -> Color.WHITE,
    "red"     -> Color.RED,
    "pink"    -> Color.PINK,
    "orange"  -> Color.ORANGE,
    "yellow"  -> Color.YELLOW,
    "green"   -> Color.GREEN,
    "magenta" -> Color.MAGENTA,
    "cyan"    -> Color.CYAN,
    "blue"    -> Color.BLUE
  )

  def getStrip(
    ledsCount  : Int = 450,
    gpioPin    : Int = 18,
    frequencyHz: Int = 800000,
    dma        : Int = 10,
    brightness : Int = 255,
    channel    : Int = 0,
    invert     : Boolean = false,
    stripType  : LedStripType = LedStripType.WS2811_STRIP_GRB,
    cleanOnExit: Boolean = true,
  ): Ws281xLedStrip = new Ws281xLedStrip(ledsCount, gpioPin, frequencyHz, dma, brightness, channel,
    invert, stripType, cleanOnExit)

}
