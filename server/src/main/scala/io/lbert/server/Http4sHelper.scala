package io.lbert.server

import io.circe.{Decoder, Encoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, EntityEncoder}
import zio.Task
import zio.interop.catz._

object Http4sHelper {

  implicit def stringEntityEncoder: EntityEncoder[Task, String] = EntityEncoder.stringEncoder

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[Task, A] =
    org.http4s.circe.jsonOf[Task, A]
  implicit def circeJsonEncoder[A](implicit decoder: Encoder[A]): EntityEncoder[Task, A] =
    org.http4s.circe.jsonEncoderOf[Task, A]

  implicit def streamEncoder[A: EntityEncoder[Task, *]]: EntityEncoder[Task, fs2.Stream[Task, A]] =
    EntityEncoder.streamEncoder[Task, A]

  implicit def byteArrayEntityEncoder: EntityEncoder[Task, Array[Byte]] = EntityEncoder.byteArrayEncoder

  val http4sDsl: Http4sDsl[Task] = Http4sDsl[Task]
}