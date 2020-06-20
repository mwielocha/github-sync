package io.mwielocha.githubsync.service

import io.mwielocha.githubsync.persistence.RepositoryStore
import com.typesafe.scalalogging.LazyLogging
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.Done
import akka.stream.scaladsl.Source
import io.mwielocha.githubsync.model.Repository
import akka.NotUsed
import akka.stream.scaladsl.Sink

class GithubSyncService(
  store: RepositoryStore,
  github: GithubRemoteService
)(implicit actorSystem: ActorSystem)
    extends LazyLogging {

  def start(): Future[Done] =
    github.source.runWith(store.sink)

}
