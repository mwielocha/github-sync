package io.mwielocha.githubsync.service

import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.duration._
import io.mwielocha.githubsync.model.Error
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
import io.circe.Decoder

class GithubRemoteService(
  http: HttpExt,
  auth: GithubAuth
)(
  implicit
  val actorSystem: ActorSystem
) extends LazyLogging
    with ErrorAccumulatingCirceSupport {

  import actorSystem.dispatcher

  private val pool = http.cachedHostConnectionPoolHttps[Unit](host = "api.github.com")

  def basicAuth: Option[BasicHttpCredentials] =
    for {
      username <- auth.username
      authtoken <- auth.authtoken
    } yield BasicHttpCredentials(username, authtoken)

  def headers: Seq[HttpHeader] =
    basicAuth.map(Authorization(_)).to(Seq)

  def unmarshall[T : Decoder](response: HttpResponse): Future[T] =
    Unmarshal(response.entity).to[T]

  // for /search
  def slowRate: Int =
    basicAuth match {
      case None    => 10
      case Some(_) => 30
    }

  def fastRate: Int =
    basicAuth match {
      case None => 60
      case Some(_) => 5000
    }

  private val call: Call = { uri =>

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


  private val isNextLink: LinkParam => Boolean = {
    case LinkParams.rel("next") => true
    case _                      => false
  }

  private [service] def unfold[T : Decoder](uri: Uri)(empty: => T): UnfoldAsync[T] = unfold(call, uri)(empty)

  private [service] def unfold[T : Decoder](call: Call, uri: Uri)(empty: => T): UnfoldAsync[T] =
    call(uri).flatMap {

      case Success(response @ HttpResponse(StatusCodes.OK, _, _, _)) =>
        processResponse(uri, response)

      case Success(response) =>
        processErrorResponse(uri, response)(empty)

      case Failure(e) =>
        logger.error("Error on request", e)
        throw e
    }

  def processResponse[T : Decoder](uri: Uri, response: HttpResponse): UnfoldAsync[T] =
    (for {
      result <- OptionT.liftF(unmarshall[T](response))
      header <- OptionT.fromOption[Future](response.header[Link])
      link <- OptionT.fromOption[Future] {
        header.values.find(_.params.exists(isNextLink))
      }
    } yield uri.withQuery(link.uri.query()) -> result).value

  def processErrorResponse[T : Decoder](uri: Uri, response: HttpResponse)(empty: => T): UnfoldAsync[T] = {
    for {
      error <- Unmarshal(response.entity).to[Error]
      _ = logger.error("Got error response from github: {}", error.message)
    } yield Some(uri -> empty)
  }
}
