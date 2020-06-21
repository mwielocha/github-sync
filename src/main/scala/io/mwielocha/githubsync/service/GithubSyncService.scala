package io.mwielocha.githubsync.service

import io.mwielocha.githubsync.persistence.{ RepositoryStore, IssueStore, EtagStore }
import com.typesafe.scalalogging.LazyLogging
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.Done
import akka.stream.scaladsl.Source
import io.mwielocha.githubsync.model.{ RepositoryIssue, Repository }
import akka.NotUsed
import akka.stream.scaladsl.Sink
import io.mwielocha.githubsync.model.{ Search, Issue }

class GithubSyncService(
  repos: GithubRepositoryService,
  issues: GithubIssueService,
  repoStore: RepositoryStore,
  issueStore: IssueStore,
  etagStore: EtagStore
)(implicit actorSystem: ActorSystem)
    extends LazyLogging {

  import actorSystem.dispatcher

  def start(): Future[Done] =
    repos.source
      .mapConcat {
        case Resource(Search(repos, _), _, _) =>
          repos
      }.runFoldAsync(Done) { (_, repo) =>
        for {
          _ <- repoStore.insertOrUpdate(repo)
          done <- issues.source(repo, etagStore.find).runFoldAsync(Done) {
            case (_, Resource(issues, uri, Some(etag))) =>
              for {
                _ <- etagStore.insertOrUpdate(uri, etag)
                _ <- persistAll(repo, issues)
              } yield Done
            case (_, Resource(issues, uri, None)) =>
              persistAll(repo, issues)
          }
        } yield done
      }

  private def persistAll(repo: Repository, issues: Seq[Issue]): Future[Done.type] =
    Source(issues).runFoldAsync(Done) { (_, issue) =>
      for {
        _ <- issueStore.insertOrUpdate(repo.id -> issue)
      } yield Done
    }
}
