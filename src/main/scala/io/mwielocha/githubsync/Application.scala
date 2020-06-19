package io.mwielocha.githubsync

import akka.actor.ActorSystem
import io.mwielocha.githubsync.http.Routes

class Application(
  val routes: Routes
)(
  implicit
    val actorSystem: ActorSystem
)
