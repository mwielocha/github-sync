package io.mwielocha.githubsync.service

import io.mwielocha.githubsync.persistence.{ RepositoryStore, IssueStore }
import com.typesafe.scalalogging.LazyLogging
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.Done
import akka.stream.scaladsl.Source
import io.mwielocha.githubsync.model.{ RepositoryIssue, Repository }
import akka.NotUsed
import akka.stream.scaladsl.Sink

class GithubSyncService(
  repos: GithubRepositoryService,
  issues: GithubIssueService,
  repoStore: RepositoryStore,
  issueStore: IssueStore
)(implicit actorSystem: ActorSystem)
    extends LazyLogging {

  import actorSystem.dispatcher

  def start(): Future[Done] =
    repos.source.runFoldAsync(Done) { (_, repo) =>
      for {
        _ <- repoStore.insertOrUpdate(repo)
        done <- issues.source(repo).map {
          repo.id -> _
        }.runFoldAsync(Done) { (_, issue) =>
          for {
            _ <- issueStore.insertOrUpdate(issue)
          } yield Done
        }
      } yield done
    }

}
