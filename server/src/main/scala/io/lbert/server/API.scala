package io.lbert.server

import org.http4s.{HttpRoutes, Response}
import zio.{Has, IO, Task, ZLayer}
import zio.interop.catz._
import Http4sHelper._
import io.circe.Json
import io.lbert.rasberry.{Animation, Color}
import io.lbert.rasberry.GPIOModule.{Brightness, Pixel, PixelIndex}
import io.lbert.rasberry.GPIOStream.{HasMessageStream, MessageStream}
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

  val live: ZLayer[LEDService with Logging with HasMessageStream, Nothing, Has[API]] = ZLayer.fromFunction { env =>
    def withLedService(func: LEDService.Service => IO[LEDServiceModule.Error, Unit]): Task[Response[Task]] =
      func(env.get).foldM(
        e => InternalServerError(errorJson(e.toString)),
        _ => NoContent()
      )

    API(HttpRoutes.of[Task] {
      case GET -> Root / "health" =>
        Ok("OK")
      case POST -> Root / "led" / "all" / "color" / color =>
        Color.fromString(color) match {
          case Some(value) => withLedService(_.setAll(value))
          case None => BadRequest(errorJson(s"Invalid color [$color]"))
        }
      case POST -> Root / "led" / num / "color" / color =>
        (Color.fromString(color), Try(num.toInt).toOption) match {
          case (Some(value), Some(index)) =>
            withLedService(_.set(Pixel(PixelIndex(index), value)))
          case _ => BadRequest(errorJson(s"Invalid color [$color] or index [$num]"))
        }

      case POST -> Root / "led" / "brightness" / num =>
        Try(num.toInt).toOption match {
          case Some(brightness) =>
            withLedService(_.setBrightness(Brightness(brightness)))
          case _ => BadRequest(errorJson(s"Invalid brightness [$num]"))
        }
      case req @ POST -> Root / "led" / "animate" / name =>
        val d = req.params.get("duration")
        (d.flatMap(d => Try(ScalaDuration(d)).toOption), name) match {
          case (Some(duration), "sequence") =>
            withLedService(_.animate(Animation.Sequence(Duration.fromScala(duration))))
          case (Some(duration), "wipe") =>
            withLedService(_.animate(Animation.Wipe(Duration.fromScala(duration))))
          case _ => BadRequest(errorJson(s"Unknown animation [$name] or duration [$d]"))
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

  val any: ZLayer[Has[API], Nothing, Has[API]] =
    ZLayer.requires[Has[API]]

  def errorJson(msg: String): Json = Json.obj(
    "error" -> Json.fromString(msg)
  )

}

case class API(api: HttpRoutes[Task])