package io.mwielocha.githubsync.service

import org.scalatest.matchers.should.Matchers
import org.mockito.MockitoSugar
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import akka.stream.scaladsl.Source
import io.mwielocha.githubsync.model.Search
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Sink
import org.scalatest.flatspec.AsyncFlatSpecLike
import io.mwielocha.githubsync.persistence.EtagStore
import io.mwielocha.githubsync.persistence.IssueStore
import io.mwielocha.githubsync.persistence.RepositoryStore

class GithubSyncServiceSpec extends TestKit(ActorSystem("GithubSyncServiceSpec"))
    with AsyncFlatSpecLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll
    with Fixtures {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val service = new GithubSyncService(
    mock[EtagStore],
    mock[IssueStore],
    mock[GithubIssueService],
    mock[RepositoryStore],
    mock[GithubRepositoryService]
  )

  "Stream graph" should "not deadlock" in {

    val uri = Uri("/repositories")

    val repoIn = Source.single(Resource(Search(Seq(repository), 1), uri))
    val issueIn = Source.single(Resource(Seq(issue), uri))

    val graph = service.graph(
      repoIn,
      _ => issueIn,
      Sink.head,
      Sink.ignore,
      Sink.ignore
    )

    graph.run().map {
      _ shouldBe repository
    }
  }
}
