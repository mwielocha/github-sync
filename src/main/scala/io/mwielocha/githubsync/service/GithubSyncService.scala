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
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Partition
import akka.stream.scaladsl.Broadcast
import akka.stream.ClosedShape
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.model.Uri
import akka.stream.SinkShape
import akka.stream.scaladsl.RestartSource
import akka.stream.SourceShape
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure

class GithubSyncService(
  repos: GithubRepositoryService,
  issues: GithubIssueService,
  repoStore: RepositoryStore,
  issueStore: IssueStore,
  etagStore: EtagStore
)(implicit actorSystem: ActorSystem)
    extends LazyLogging {

  import actorSystem.dispatcher

  def start(): Future[Done] = {

    logger.info("Starting stream...")

    graph(
      repos.source,
      repository =>
        issues
          .source(repository, etagStore.find),
      repoStore.sink,
      issueStore.sink,
      etagStore.sink
    ).run()
  }

  def loop(): Unit =
    start().onComplete {
      case Success(_) =>
        logger.info("Stream finished.")
        actorSystem.scheduler.scheduleOnce(10 seconds)(loop)
      case Failure(e) =>
        logger.error("Error while streaming from github", e)
    }

  type Issues = Resource[Seq[Issue]]
  type Repositories = Resource[Search[Repository]]

  def graph[Out](
    repoIn: Source[Repositories, NotUsed],
    issueIn: Repository => Source[Issues, NotUsed],
    repoOut: Sink[Repository, Future[Done]],
    issueOut: Sink[(Repository.Id, Issue), Out],
    etagOut: Sink[(Uri, EntityTag), Future[Done]]
  ): RunnableGraph[Out] = RunnableGraph.fromGraph {

    GraphDSL.create(issueOut) { implicit builder => issueOut =>
      import GraphDSL.Implicits._

      val repoSplitter = builder.add(Broadcast[Repository](2))

      val issueSplitter = builder.add(Broadcast[(Repository, Issues)](2))

      val etagFlow = Flow[(Repository, Issues)]
        .flatMapConcat {
          case (_, Resource(_, uri, Some(etag))) =>
            Source.single(uri -> etag)
          case _ =>
            Source.empty
        }

      val issueFlow = Flow[(Repository, Issues)]
        .mapConcat[(Repository.Id, Issue)] {
          case (repo, Resource(issues, _, _)) =>
            issues.map(repo.id -> _)
        }

      val issueInFlow = Flow[Repository]
        .flatMapConcat { repo =>
          issueIn(repo).map {
            repo -> _
          }
        }

      val repoFlow = Flow[Repositories]
        .mapConcat {
          _.data.items
        }

      val in = RestartSource
        .onFailuresWithBackoff(
          minBackoff = 2 seconds,
          maxBackoff = 10 seconds,
          randomFactor = 0.2
        )(() => repoIn)

      in ~> repoFlow ~> repoSplitter.in

      repoSplitter ~> issueInFlow ~> issueSplitter.in

      repoSplitter ~> repoOut

      issueSplitter ~> etagFlow ~> etagOut

      issueSplitter ~> issueFlow ~> issueOut

      ClosedShape
    }
  }
}
