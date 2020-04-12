package io.lbert.server

import org.http4s.HttpRoutes
import zio.{Has, Task, ZLayer}
import zio.interop.catz._
import Http4sHelper._
import io.circe.Json
import io.lbert.rasberry.Color
import io.lbert.server.LEDServiceModule.LEDService
import zio.logging.Logging

object API {

  import http4sDsl._

  val live: ZLayer[LEDService with Has[Logging], Nothing, Has[API]] = ZLayer.fromFunction(env =>
    API(HttpRoutes.of[Task] {
      case GET -> Root / "health" =>
        Ok("OK")
      case POST -> Root / "led" / "color" / color =>
        Color.fromString(color) match {
          case Some(value) =>
            env.get[Logging].logger.log(s"Changing color to [$value]") *>
            env.get.setAll(value).foldM(
              e => InternalServerError(errorJson(e.toString)),
              _ => NoContent()
            )
          case None => BadRequest(errorJson(s"Invalid color [$color]"))
        }
    })
  )

  val any: ZLayer[Has[API], Nothing, Has[API]] =
    ZLayer.requires[Has[API]]

  def errorJson(msg: String): Json = Json.obj(
    "error" -> Json.fromString(msg)
  )

}

case class API(api: HttpRoutes[Task])