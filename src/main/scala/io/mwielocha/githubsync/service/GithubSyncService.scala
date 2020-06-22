package io.mwielocha.githubsync.service

import io.mwielocha.githubsync.persistence.{ RepositoryStore, IssueStore, EtagStore }
import com.typesafe.scalalogging.LazyLogging
import akka.actor.ActorSystem
import scala.concurrent.Future
import akka.Done
import akka.stream.scaladsl.Source
import io.mwielocha.githubsync.model.Repository
import akka.NotUsed
import akka.stream.scaladsl.Sink
import io.mwielocha.githubsync.model.{ Search, Issue }
import akka.stream.scaladsl.RunnableGraph
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Broadcast
import akka.stream.ClosedShape
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.RestartSource
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure

class GithubSyncService(
  etagStore: EtagStore,
  issueStore: IssueStore,
  issueService: GithubIssueService,
  repositoryStore: RepositoryStore,
  repositoryService: GithubRepositoryService
)(implicit actorSystem: ActorSystem)
    extends LazyLogging {

  private val streamDelay = 5 seconds

  import actorSystem.dispatcher

  def start(): Future[Done] = {

    logger.info("Starting stream...")

    graph(
      repositoryService.source,
      repository =>
        issueService
          .source(repository, etagStore.find),
      repositoryStore.sink,
      issueStore.sink,
      etagStore.sink
    ).run()
  }

  def loop(): Unit =
    start().onComplete {
      case Success(_) =>
        logger.info("Stream finished, scheduling new run in {}", streamDelay)
        actorSystem.scheduler.scheduleOnce(streamDelay)(loop)
      case Failure(e) =>
        logger.error("Error while streaming from github", e)
    }

  type Issues = Resource[Seq[Issue]]
  type Repositories = Resource[Search[Repository]]

  def graph[Out](
    repoIn: Source[Repositories, NotUsed],
    issueIn: Repository => Source[Issues, NotUsed],
    repoOut: Sink[Repository, Out],
    issueOut: Sink[(Repository.Id, Issue), Future[Done]],
    etagOut: Sink[(Uri, EntityTag), Future[Done]]
  ): RunnableGraph[Out] = RunnableGraph.fromGraph {

    GraphDSL.create(repoOut) { implicit builder => repoOut =>
      import GraphDSL.Implicits._

      /*                    |---|
       *         |-----| ~> |out| ~> //
       *         |     |    |---|                 |----|    |---|
       * repo ~> |split|               |-----| ~> |etag| ~> |out|
       *         |     |    |-----|    |     |    |----|    |---|
       *         |-----| ~> |issue| ~> |split|
       *                    |-----|    |     |    |---|
       *                               |-----| ~> |out|
       *                                          |---|
       */

      val repoSplitter = builder.add(Broadcast[Repository](2))

      val issueSplitter = builder.add(Broadcast[(Repository, Issues)](2))

      val etagFlow = Flow[(Repository, Issues)]
        .flatMapConcat {
          case (_, Resource(_, uri, Some(etag))) =>
            Source.single(uri -> etag)
          case _ =>
            Source.empty
        }

      val issueExtractFlow = Flow[(Repository, Issues)]
        .mapConcat[(Repository.Id, Issue)] {
          case (repo, Resource(issues, _, _)) =>
            issues.map(repo.id -> _)
        }

      val issueFlatMapFlow = Flow[Repository]
        .flatMapConcat { repo =>
          issueIn(repo).map {
            repo -> _
          }
        }

      val repoFlow = Flow[Repositories]
        .mapConcat {
          _.data.items
        }

      val restartingIn = RestartSource
        .onFailuresWithBackoff(
          minBackoff = 2 seconds,
          maxBackoff = 10 seconds,
          randomFactor = 0.2
        )(() => repoIn)

      restartingIn ~> repoFlow ~> repoSplitter.in

      repoSplitter ~> issueFlatMapFlow ~> issueSplitter.in

      repoSplitter ~> repoOut

      issueSplitter ~> etagFlow ~> etagOut

      issueSplitter ~> issueExtractFlow ~> issueOut

      ClosedShape
    }
  }
}
