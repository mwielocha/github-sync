package io.mwielocha.githubsync.blueprints

import factorio._
import akka.actor.ActorSystem

@blueprint
trait AkkaBlueprint {

  @provides
  def actorSystem: ActorSystem =
    ActorSystem("githubsync-system")

}
