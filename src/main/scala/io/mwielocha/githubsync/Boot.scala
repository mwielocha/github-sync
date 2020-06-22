package io.mwielocha.githubsync

import com.typesafe.scalalogging.LazyLogging
import factorio._
import io.mwielocha.githubsync.blueprints.AppBlueprint
import scala.concurrent.Await
import scala.concurrent.duration._
import pureconfig._
import pureconfig.generic.auto._
import io.mwielocha.githubsync.config.AppConfig
import pureconfig.error.ConfigReaderFailures

object Boot extends App with LazyLogging {

  logger.info("Starting up...")

  ConfigSource.default.load[AppConfig] match {
    case Right(config) => boot(config)
    case Left(ConfigReaderFailures(head, _)) =>
      logger.error("Missconfigured: {}", head.description)
  }

  def boot(config: AppConfig) = {

    val blueprint = new AppBlueprint(config)

    val assemble = Assembler[Application](blueprint)

    val application = assemble()

    import application._
    import actorSystem.dispatcher

    for {
      _ <- http.bindAndHandle(
        routes(),
        config.host,
        config.port
      )
      _ = synchronizer.loop()
    } yield ()

    sys.addShutdownHook {
      Await.result(
        actorSystem
          .terminate(),
        1 second
      )
    }
  }
}
