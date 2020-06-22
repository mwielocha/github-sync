package io.mwielocha.githubsync.service

import akka.actor.ActorSystem
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpRequest
import io.mwielocha.githubsync.model.Error
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.util.Success
import scala.concurrent.Future
import scala.util.Failure
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.model.HttpResponse
import io.mwielocha.githubsync.config.GithubAuth
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.Authorization
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Sink
import scala.util.Try
import cats.implicits._
import akka.http.scaladsl.model.headers.Link
import akka.http.scaladsl.model.headers.LinkParam
import akka.http.scaladsl.model.headers.LinkParams
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.HttpExt
import io.circe.Decoder
import akka.http.scaladsl.model.headers.{ ETag, `If-None-Match` }
import akka.http.scaladsl.model.headers.EntityTag
import scala.concurrent.duration._

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

  def headers(etag: Option[EntityTag]): Seq[HttpHeader] =
    Seq.empty[HttpHeader] ++
      basicAuth.map(Authorization(_)) ++
      etag.map(`If-None-Match`(_))

  def unmarshall[T : Decoder](response: HttpResponse): Future[T] =
    Unmarshal(response.entity).to[T]

  // for /search
  def slowRate: Rate =
    basicAuth match {
      case None    => Rate(10, 1 minute)
      case Some(_) => Rate(30, 1 minute)
    }

  def fastRate: Rate =
    basicAuth match {
      case None    => Rate(60, 1 hour)
      case Some(_) => Rate(5000, 1 hour)
    }

  private[service] val call: Call = { (uri, etag) =>
    logger.debug("Submitting request for: {}", uri)

    Source
      .single(
        HttpRequest(uri = uri)
          .withHeaders(headers(etag)) -> ()
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

  private[service] def extractLink(uri: Uri, response: HttpResponse): Option[Uri] =
    for {
      header <- response.header[Link]
      link <- header.values.find(_.params.exists(isNextLink))
    } yield uri.withQuery(link.uri.query())

  private[service] def extractEtag(response: HttpResponse): Option[EntityTag] =
    for {
      header <- response.header[ETag]
    } yield header.etag

  private[service] def unfold[T : Decoder](
    next: Option[Uri],
    getEtag: GetEtag,
    empty: => T
  ): UnfoldAsync[T] = unfold(call, next, getEtag, empty)

  private[service] def unfold[T : Decoder](
    call: Call,
    next: Option[Uri],
    getEtag: GetEtag,
    empty: => T
  ): UnfoldAsync[T] =
    next.fold(Future.successful(None): UnfoldAsync[T]) { uri =>
      for {
        etag <- getEtag(uri)
        response <- call(uri, etag)
        result <- process(uri, response, empty)
      } yield result
    }

  private def process[T : Decoder](
    uri: Uri,
    response: Try[HttpResponse],
    empty: => T
  ): UnfoldAsync[T] = response match {

    case Success(response @ HttpResponse(StatusCodes.OK, _, _, _)) =>
      processResponse(uri, response)

    case Success(response @ HttpResponse(StatusCodes.NotModified, _, _, _)) =>
      processNotModifiedResponse(uri, response, empty)

    case Success(response) =>
      processErrorResponse(uri, response, empty)

    case Failure(e) =>
      logger.error("Error on request", e)
      throw e

  }

  def processResponse[T : Decoder](uri: Uri, response: HttpResponse): UnfoldAsync[T] =
    unmarshall[T](response).map { result =>
      (extractLink(uri, response) ->
        Resource(result, uri, extractEtag(response))).some
    }

  def processErrorResponse[T : Decoder](
    uri: Uri,
    response: HttpResponse,
    empty: => T
  ): UnfoldAsync[T] = {
    for {
      error <- Unmarshal(response.entity).to[Error]
      _ = logger.error("Got error response from github: {}", error.message)
    } yield Some(uri.some -> Resource(empty, uri))
  }

  def processNotModifiedResponse[T : Decoder](
    uri: Uri,
    response: HttpResponse,
    empty: => T
  ): UnfoldAsync[T] =
    Future.successful {
      logger.debug("Not modified: {}", uri)
      val link = extractLink(uri, response)
      (link -> Resource(empty, uri, None)).some
    }

}
