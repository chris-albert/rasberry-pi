package io.lbert.server

import org.http4s.HttpRoutes
import zio.{Has, Task, ZLayer}
import zio.interop.catz._
import Http4sHelper._

object API {

  import http4sDsl._

  val live: ZLayer[Any, Nothing, Has[API]] = ZLayer.succeed(
    API(HttpRoutes.of[Task] {
      case GET -> Root / "health" =>
        Ok("OK")
    })
  )

  val any: ZLayer[Has[API], Nothing, Has[API]] =
    ZLayer.requires[Has[API]]

}

case class API(api: HttpRoutes[Task])