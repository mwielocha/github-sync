package io.mwielocha.githubsync

import akka.actor.ActorSystem
import io.mwielocha.githubsync.http.Routes
import io.mwielocha.githubsync.service.GithubSyncService
import akka.http.scaladsl.HttpExt

class Application(
  val http: HttpExt,
  val routes: Routes,
  val synchronizer: GithubSyncService
)(
  implicit
  val actorSystem: ActorSystem
)
