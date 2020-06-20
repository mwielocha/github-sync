package io.mwielocha.githubsync.blueprints

import factorio._
import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt

@blueprint
trait HttpBlueprint {

  @provides
  def http(implicit actorSystem: ActorSystem): HttpExt =
    Http()

}
