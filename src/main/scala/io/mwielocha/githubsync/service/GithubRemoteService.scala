package io.mwielocha.githubsync.service

import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.duration._
import io.mwielocha.githubsync.model.Search
import io.mwielocha.githubsync.model.Error
import io.mwielocha.githubsync.model.Repository
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.util.Success
import scala.concurrent.Future
import scala.util.Failure
import com.typesafe.scalalogging.LazyLogging
import akka.NotUsed
import akka.http.scaladsl.common.JsonEntityStreamingSupport
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.model.HttpResponse
import io.mwielocha.githubsync.config.GithubAuth
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.stream.ThrottleMode
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Sink
import scala.util.Try
import cats.implicits._
import akka.http.scaladsl.model.headers.Link
import akka.http.scaladsl.model.headers.LinkParam
import akka.http.scaladsl.model.headers.LinkParams
import cats.data.OptionT
import akka.http.scaladsl.model.StatusCodes
import scala.collection.Searching.SearchResult
import akka.http.scaladsl.HttpExt

class GithubRemoteService(
  http: HttpExt,
  auth: GithubAuth
)(
  implicit
  private val actorSystem: ActorSystem
) extends LazyLogging
    with ErrorAccumulatingCirceSupport {

  import actorSystem.dispatcher

  private val pool = http.cachedHostConnectionPoolHttps[Unit](host = "api.github.com")

  private def basicAuth: Option[BasicHttpCredentials] =
    for {
      username <- auth.username
      authtoken <- auth.authtoken
    } yield BasicHttpCredentials(username, authtoken)

  private def headers: Seq[HttpHeader] =
    basicAuth.map(Authorization(_)).to(Seq)

  private def unmarshall(response: HttpResponse): Future[Search[Repository]] =
    Unmarshal(response.entity).to[Search[Repository]]

  private def rate: Int =
    basicAuth match {
      case None    => 10
      case Some(_) => 30
    }

  private [service] type GetPage = Uri => Future[Try[HttpResponse]]

  private val getPage: GetPage = { uri =>

    logger.debug("Submitting request for: {}", uri)

    Source
      .single(
        HttpRequest(uri = uri)
          .withHeaders(headers) -> ()
      )
      .via(pool).map {
        case (response, _) =>
          response
      }.runWith(Sink.head)
  }

  private [service] val baseUri = Uri("/search/repositories")

  private [service] val baseQuery = baseUri.withQuery(
    Uri.Query(
      "q" -> "good-first-issues:>1 language:haskell",
      "sort" -> "help-wanted-issues",
      "order" -> "desc",
      "per_page" -> "50"
    )
  )

  private val isNextLink: LinkParam => Boolean = {
    case LinkParams.rel("next") => true
    case _                      => false
  }

  private type UnfoldAsync = Future[Option[(Uri, Search[Repository])]]

  private [service] def unfold(uri: Uri): UnfoldAsync = unfold(getPage, uri)

  private [service] def unfold(getPage: GetPage, uri: Uri): UnfoldAsync =
    getPage(uri).flatMap {

      case Success(response @ HttpResponse(StatusCodes.OK, _, _, _)) =>
        processResponse(response)

      case Success(response) =>
        processErrorResponse(uri, response)

      case Failure(e) =>
        logger.error("Error on request", e)
        throw e
    }

  private[service] def processResponse(response: HttpResponse): UnfoldAsync =
    (for {
      result <- OptionT.liftF(unmarshall(response))
      header <- OptionT.fromOption[Future](response.header[Link])
      link <- OptionT.fromOption[Future] {
        header.values.find(_.params.exists(isNextLink))
      }
    } yield baseUri.withQuery(link.uri.query()) -> result).value

  private [service] def processErrorResponse(uri: Uri, response: HttpResponse): UnfoldAsync = {
    for {
      error <- Unmarshal(response.entity).to[Error]
      _ = logger.error("Got error response from github: {}", error.message)
    } yield Some(uri -> Search[Repository](Nil, 0))
  }

  def source: Source[Repository, NotUsed] =
    Source
      .unfoldAsync[Uri, Search[Repository]](baseQuery)(unfold)
      .map(_.items)
      .throttle(rate, 1 minute, 1, ThrottleMode.Shaping)
      .mapConcat(identity)

}
