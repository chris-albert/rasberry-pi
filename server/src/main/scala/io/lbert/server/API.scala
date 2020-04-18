package io.lbert.server

import org.http4s.{HttpRoutes, Response}
import zio.{Has, IO, Task, ZLayer}
import zio.interop.catz._
import Http4sHelper._
import io.circe.Json
import io.lbert.rasberry.{Animation, Color}
import io.lbert.rasberry.GPIOModule.{Brightness, Pixel, PixelIndex}
import io.lbert.rasberry.GPIOStream.{HasMessageStream, MessageStream}
import io.lbert.server.AnimationJobModule.AnimationJob
import io.lbert.server.LEDServiceModule.LEDService
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import zio.duration.Duration
import zio.logging.log._
import zio.logging.Logging.Logging
import scala.concurrent.duration.{Duration => ScalaDuration}
import scala.util.Try

object API {

  import http4sDsl._

  val noOpPipe: fs2.Pipe[Task, WebSocketFrame, Unit] = _.evalMap(_ => IO.unit)

  val live: ZLayer[LEDService with Logging with HasMessageStream with AnimationJob, Nothing, Has[API]] = ZLayer.fromFunction { env =>
    def withLedService(func: LEDService.Service => IO[LEDServiceModule.Error, Unit]): Task[Response[Task]] =
      func(env.get).foldM(
        e => InternalServerError(errorJson(e.toString)),
        _ => NoContent()
      )

    API(HttpRoutes.of[Task] {
      case GET -> Root / "health" =>
        Ok("OK")
      case req @ POST -> Root / "led" / "solid" =>
        getColor(req.params) match {
          case Left(value) => BadRequest(errorJson(s"Invalid color [$value]"))
          case Right(color) =>
            getIndex(req.params) match {
              case Some(pixels) =>
                withLedService(_.set(pixels.map(Pixel(_, color))))
              case None =>
                withLedService(_.setAll(color))
            }
        }
      case POST -> Root / "led" / "brightness" / num =>
        Try(num.toInt).toOption match {
          case Some(brightness) =>
            withLedService(_.setBrightness(Brightness(brightness)))
          case _ => BadRequest(errorJson(s"Invalid brightness [$num]"))
        }
      case POST -> Root / "led" / "animate" / "stop" =>
        AnimationJob.stopJob().provide(env).flatMap(_ => NoContent())
      case req @ POST -> Root / "led" / "animate" / name =>
        getAnimation(name, req.params) match {
          case Left(value) => BadRequest(errorJson(value))
          case Right(value) => withLedService(s => AnimationJob.setJob(s.animate(value)).provide(env))
        }
      case GET -> Root / "subscribe" =>
        env.get[MessageStream].flatMap { stream =>
          val c                            = stream.map(m => m.toString)
          val cc: fs2.Stream[Task, String] = Fs2StreamInterop.toFs2(c)
            .evalMap[Task, String](o => Task(println(s"In subscribe, got message [$o]")).as(o))

          WebSocketBuilder[Task].build(
            cc.map(s => Text(s)),
            noOpPipe
          )
        }
    })
  }

  private def getAnimation(name: String, query: Map[String, String]): Either[String, Animation] = {
    name match {
      case "sequence" =>
        getDuration(query).map(Animation.Sequence)
      case "wipe" =>
        getDuration(query).map(Animation.Wipe)
      case "rainbow" =>
        getDuration(query).map(Animation.Rainbow)
      case "theater" =>
        for {
          d <- getDuration(query)
          c <- getColor(query)
        } yield Animation.TheaterChase(
          d,
          c,
          getColor(query, "backgroundColor").getOrElse(Color.Black),
          getInt(query, "channels").getOrElse(3),
          getBool(query, "flip").getOrElse(false),
          getInt(query, "count").getOrElse(1)
        )
      case _ => Left(s"No animation found for [$name]")
    }
  }

  private def getDuration(query: Map[String, String]): Either[String, Duration] =
    for {
      d  <- query.get("duration").toRight("No duration defined")
      sd <- Try(ScalaDuration(d)).toOption.toRight(s"Invalid duration [$d]")
    } yield Duration.fromScala(sd)

  private def getColor(query: Map[String, String], name: String = "color"): Either[String, Color] =
    query.get(name) match {
      case Some(c) => Color.fromString(c).toRight(s"Invalid color [$c]")
      case None =>
        (getInt(query, "r"), getInt(query, "g"), getInt(query, "b")) match {
          case (Some(r), Some(g), Some(b)) => Right(Color(r, g, b))
          case _ => Left(s"Invalid color selection")
        }
    }

  private def getIndex(query: Map[String, String]): Option[List[PixelIndex]] =
    getInt(query, "pixel") match {
      case Some(value) => Some(List(PixelIndex(value)))
      case None =>
        (getInt(query, "pixelStart"), getInt(query, "pixelFinish")) match {
          case (Some(s), Some(f)) => Some((s to f).map(PixelIndex).toList)
          case _ => None
        }
    }

  private def getInt(query: Map[String, String], key: String): Option[Int] =
    query.get(key).flatMap(s => Try(s.toInt).toOption)

  private def getBool(query: Map[String, String], key: String): Option[Boolean] =
    query.get(key).flatMap(s => Try(s.toBoolean).toOption)

  val any: ZLayer[Has[API], Nothing, Has[API]] =
    ZLayer.requires[Has[API]]

  def errorJson(msg: String): Json = Json.obj(
    "error" -> Json.fromString(msg)
  )

}

case class API(api: HttpRoutes[Task])