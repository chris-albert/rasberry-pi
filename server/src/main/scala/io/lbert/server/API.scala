package io.lbert.server

import org.http4s.HttpRoutes
import zio.{ Has, IO, Task, ZLayer}
import zio.interop.catz._
import Http4sHelper._
import io.circe.Json
import io.lbert.rasberry.Color
import io.lbert.server.GPIOQueue.{Message, MessageStream}
import io.lbert.server.LEDServiceModule.LEDService
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import zio.logging.log._
import zio.logging.Logging.Logging
import zio.stream.Stream

object API {

  import http4sDsl._

  val noOpPipe: fs2.Pipe[Task, WebSocketFrame, Unit] = _.evalMap(_ => IO.unit)

  val live: ZLayer[LEDService with Logging with MessageStream, Nothing, Has[API]] = ZLayer.fromFunction(env =>
    API(HttpRoutes.of[Task] {
      case GET -> Root / "health" =>
        Ok("OK")
      case POST -> Root / "led" / "color" / color =>
        Color.fromString(color) match {
          case Some(value) =>
            info(s"Changing color to [$value]").provide(env) *>
            env.get.setAll(value).foldM(
              e => InternalServerError(errorJson(e.toString)),
              _ => NoContent()
            )
          case None => BadRequest(errorJson(s"Invalid color [$color]"))
        }

      case GET -> Root / "subscribe" =>
        val b = env.get[Stream[Nothing, Message]]

        val c = b.map(m => m.toString)
        val cc: fs2.Stream[Task, String] = Fs2StreamInterop.toFs2(c)
          .evalMap[Task, String](o => Task(println(s"In subscribe, got message [$o]")).as(o))

        WebSocketBuilder[Task].build(
          cc.map(s => Text(s)),
          noOpPipe
        )
        
      case GET -> Root / "subscribe2" =>
        val b = env.get[Stream[Nothing, Message]]

        val c = b.map(m => m.toString)
        val cc: fs2.Stream[Task, String] = Fs2StreamInterop.toFs2(c)
          .evalMap[Task, String](o => Task(println(s"In subscribe, got message [$o]")).as(o))

        WebSocketBuilder[Task].build(
          cc.map(s => Text(s)),
          noOpPipe
        )
    })
  )

  val any: ZLayer[Has[API], Nothing, Has[API]] =
    ZLayer.requires[Has[API]]

  def errorJson(msg: String): Json = Json.obj(
    "error" -> Json.fromString(msg)
  )

}

case class API(api: HttpRoutes[Task])