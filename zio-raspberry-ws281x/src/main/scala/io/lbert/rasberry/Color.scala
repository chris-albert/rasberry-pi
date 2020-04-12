package io.lbert.rasberry

import com.github.mbelling.ws281x.{Color => LEDColor}

final case class Color(
  red  : Int,
  green: Int,
  blue : Int
)

object Color {

  val colorMapping = List(
    "white"      -> Color(255, 255, 255),
    "light_gray" -> Color(192, 192, 192),
    "gray"       -> Color(128, 128, 128),
    "dark_gray"  -> Color(64, 64, 64),
    "black"      -> Color(0, 0, 0),
    "red"        -> Color(255, 0, 0),
    "pink"       -> Color(255, 175, 175),
    "orange"     -> Color(255, 200, 0),
    "yellow"     -> Color(255, 255, 0),
    "green"      -> Color(0, 255, 0),
    "magenta"    -> Color(255, 0, 255),
    "cyan"       -> Color(0, 255, 255),
    "blue"       -> Color(0, 0, 255)
  )

  val stringToColor: Map[String, Color] = colorMapping.toMap
  val colorToString: Map[Color, String] = colorMapping.map(t => t._2 -> t._1).toMap

  def fromString(s: String): Option[Color] =
    stringToColor.get(s)

  def toString(c: Color): String =
    colorToString.getOrElse(c, c.toString)

  def toLEDColor(color: Color): LEDColor =
    new LEDColor(color.red, color.green, color.blue)
}
