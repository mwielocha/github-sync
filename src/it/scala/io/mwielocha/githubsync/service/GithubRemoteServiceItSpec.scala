package io.mwielocha.githubsync.service

import org.scalatest.matchers.should.Matchers
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.scalatest.flatspec.AsyncFlatSpecLike
import io.mwielocha.githubsync.config.GithubAuth
import org.scalatest.BeforeAndAfterAll
import scala.util.Success
import scala.util.Failure
import akka.http.scaladsl.model.Uri

class GithubRemoteServiceItSpec
    extends TestKit(ActorSystem("GithubRemoteServiceItSpec"))
    with AsyncFlatSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val api = new GithubRemoteService(Http(), GithubAuth())

  "Github api" should "respond with a correct link header" in {

    val params = Seq("q" -> "language:haskell", "per_page" -> "1")

    val uri = Uri("/search/repositories")
      .withQuery(Uri.Query(params: _*))

    api.call(uri, None).map {
      case Failure(_) => fail()
      case Success(response) =>
        api.extractLink(uri, response) should contain {
          uri.withQuery(Uri.Query((params :+ ("page" -> "2")): _*))
        }
    }
  }

  it should "respond with a correct etag header" in {

    val uri = Uri("/repositories")
      .withQuery(Uri.Query("per_page" -> "1"))

    api.call(uri, None).map {
      case Failure(_) => fail()
      case Success(response) =>
        api.extractEtag(response) shouldBe defined
    }
  }
}
