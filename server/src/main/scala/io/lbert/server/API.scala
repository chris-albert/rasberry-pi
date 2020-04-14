package io.lbert.server

import org.http4s.{EntityBody, HttpRoutes}
import zio.{Chunk, Has, Task, ZLayer}
import zio.interop.catz._
import Http4sHelper._
import io.circe.Json
import io.lbert.rasberry.Color
import io.lbert.server.GPIOQueue.{Message, MessageStream}
import io.lbert.server.LEDServiceModule.LEDService
import zio.logging.log._
import zio.logging.Logging.Logging
import zio.stream.Stream

object API {

  import http4sDsl._

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

        val c = b.map(m => m.toString.getBytes)
          .flatMap(a => Stream.fromChunk(Chunk.fromArray(a)))

        val body: EntityBody[Task] = Fs2StreamInterop.toFs2(c)

        val a = Ok(body)
        a
    })
  )

  val any: ZLayer[Has[API], Nothing, Has[API]] =
    ZLayer.requires[Has[API]]

  def errorJson(msg: String): Json = Json.obj(
    "error" -> Json.fromString(msg)
  )

}

case class API(api: HttpRoutes[Task])