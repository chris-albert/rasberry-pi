package io.lbert.server

import zio.{Fiber, Has, IO, Ref, UIO, URIO, ZIO, ZLayer}

object AnimationJobModule {

  type AnimationEffect = IO[LEDServiceModule.Error, Unit]
  type AnimationFiber = Fiber[LEDServiceModule.Error, Unit]
  type AnimationJob = Has[AnimationJob.Service]

  object AnimationJob {
    trait Service {
      def setJob(effect: AnimationEffect): UIO[Unit]
      def stopJob(): UIO[Unit]
    }

    val live: ZLayer[Any, Nothing, AnimationJob] = ZLayer.fromEffect(
      Ref.make[Option[AnimationFiber]](None).map(animationJob =>
        new AnimationJob.Service {

          private def stop: UIO[Unit] =
            for {
              job <- animationJob.get
              _   <- job.fold(IO.unit)(_.interrupt.unit)
            } yield ()

          override def setJob(effect: AnimationEffect): UIO[Unit] =
            effect.forkDaemon.flatMap(fiber =>
              stop *> animationJob.set(Some(fiber))
            )

          override def stopJob(): UIO[Unit] =
            stop *> animationJob.set(None)
        }
      )
    )

    def setJob(effect: AnimationEffect): URIO[AnimationJob, Unit] =
      ZIO.accessM(_.get.setJob(effect))

    def stopJob(): URIO[AnimationJob, Unit] =
      ZIO.accessM(_.get.stopJob())
  }

}
