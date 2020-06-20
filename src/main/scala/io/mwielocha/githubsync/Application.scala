package io.mwielocha.githubsync

import akka.actor.ActorSystem
import io.mwielocha.githubsync.http.Routes
import io.mwielocha.githubsync.service.GithubSyncService

class Application(
  val routes: Routes,
  val githubSync: GithubSyncService
)(
  implicit
    val actorSystem: ActorSystem
)
