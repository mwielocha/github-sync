package io.mwielocha.githubsync.blueprints

import factorio._
import akka.stream.scaladsl.Source
import io.mwielocha.githubsync.model.Repository
import io.mwielocha.githubsync.service.GithubRemoteService
import akka.NotUsed
import io.mwielocha.githubsync.persistence.RepositoryStore
import akka.stream.scaladsl.Sink
import scala.concurrent.Future
import akka.Done

@blueprint
trait ServiceBlueprint {

  // todo

}
