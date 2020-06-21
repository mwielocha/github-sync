package io.mwielocha.githubsync.service

import org.scalatest.flatspec.AsyncFlatSpec
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
import io.mwielocha.githubsync.model.User
import io.mwielocha.githubsync.model.Error
import io.circe.syntax._
import io.circe.Printer
import akka.http.scaladsl.Http
import scala.concurrent.Future
import scala.util.Try
import scala.collection.Searching.SearchResult

class GithubRemoteServiceSpec extends TestKit(ActorSystem("GithubRemoteServiceSpec")) with AsyncFlatSpecLike with Matchers with BeforeAndAfterAll {

  private val service = new GithubRemoteService(Http(), GithubAuth(None, None))

  import service.GetPage

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "GithubRemoteService" should "unfold a correct github api response" in {

    val repository = Repository(
      Repository.Id(956452),
      User(User.Id(7874557), "LambdaHack"),
      "LambdaHack",
      "https://github.com/LambdaHack/LambdaHack",
      "LambdaHack/LambdaHack",
      Some("Haskell game engine library for roguelike dungeon crawlers.")
    )

    val search = Search(Seq(repository), 1)

    val nextLinkQuery = Uri.Query("page" -> "2")

    val linkValue = LinkValue(service.baseUri.withQuery(nextLinkQuery), LinkParams.rel("next"))

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

    val getPage: GetPage =
      _ => Future.successful(Try(response))

    for {
      processed <- service.unfold(getPage, Uri(""))
    } yield processed shouldBe Some(
      service.baseUri.withQuery(nextLinkQuery) -> search
    )
  }

  it should "unfold an error github response" in {

    val json = """ { "message": "bad" } """

    val response = HttpResponse(403)
      .withEntity(ContentTypes.`application/json`, ByteString.fromString(json))

    val getPage: GetPage =
      _ => Future.successful(Try(response))

    for {
      processed <- service.unfold(getPage, Uri(""))
    } yield processed shouldBe Some(Uri("") -> Search.empty)
  }
}
