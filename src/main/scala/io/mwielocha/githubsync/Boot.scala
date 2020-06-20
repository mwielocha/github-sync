package io.mwielocha.githubsync

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCode }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import factorio._
import com.typesafe.config.ConfigFactory
import io.mwielocha.githubsync.blueprints.AppBlueprint
import factorio.annotations.blueprint
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

    import application.actorSystem

    val http = Http()

    http.bindAndHandle(application.routes(), config.host, config.port)

    sys.addShutdownHook {
      Await.result(
        application.actorSystem
          .terminate(),
        1 second
      )
    }
  }
}
