package io.mwielocha.githubsync.service

import org.scalatest.matchers.should.Matchers
import io.mwielocha.githubsync.config.GithubAuth
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.BeforeAndAfterAll
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.Uri
import io.mwielocha.githubsync.model.Search
import io.mwielocha.githubsync.model.Repository
import akka.http.scaladsl.model.ContentTypes
import akka.util.ByteString
import akka.http.scaladsl.model.headers.Link
import akka.http.scaladsl.model.headers.LinkParams
import akka.http.scaladsl.model.headers.LinkValue
import io.circe.syntax._
import io.circe.Printer
import akka.http.scaladsl.Http
import scala.concurrent.Future
import scala.util.Try
import cats.syntax.option._
import akka.http.scaladsl.model.headers.EntityTag

class GithubRemoteServiceSpec extends TestKit(ActorSystem("GithubRemoteServiceSpec")) with AsyncFlatSpecLike with Matchers with BeforeAndAfterAll with Fixtures {

  private val api = new GithubRemoteService(Http(), GithubAuth(None, None))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val baseUri = Uri("/endpoint")

  val getEtag: GetEtag = _ => Future.successful[Option[EntityTag]](None)

  "GithubRemoteService" should "unfold a correct github api response" in {

    val search = Search(Seq(repository), 1)

    val nextLinkQuery = Uri.Query("page" -> "2")

    val linkValue = LinkValue(baseUri.withQuery(nextLinkQuery), LinkParams.rel("next"))

    val linkHeader = Link(linkValue)

    val response = HttpResponse(200)
      .withEntity(
        ContentTypes.`application/json`,
        ByteString.fromString(
          search.asJson
            .printWith(Printer.spaces2)
        )
      )
      .withHeaders(Seq(linkHeader))

    val call: Call =
      (_, _) => Future.successful(Try(response))

    for {
      processed <- api.unfold[Search[Repository]](call, baseUri.some, getEtag, Search.empty[Repository])
    } yield processed should contain(
      baseUri.withQuery(nextLinkQuery).some -> Resource(search, baseUri)
    )
  }

  it should "unfold an error github response" in {

    val json = """ { "message": "bad" } """

    val response = HttpResponse(403)
      .withEntity(ContentTypes.`application/json`, ByteString.fromString(json))

    val call: Call =
      (_, _) => Future.successful(Try(response))

    for {
      processed <- api.unfold[Search[Repository]](call, baseUri.some, getEtag, Search.empty[Repository])
    } yield processed should contain(baseUri.some -> Resource(Search.empty, baseUri))
  }
}
