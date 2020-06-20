package io.mwielocha.githubsync.service

import akka.http.scaladsl.Http
import akka.actor.ActorSystem
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.HttpRequest
import scala.concurrent.duration._
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

class GithubRemoteService(
  auth: GithubAuth
)(
  implicit
  private val actorSystem: ActorSystem
) extends LazyLogging
    with ErrorAccumulatingCirceSupport {

  import actorSystem.dispatcher

  private val pool = Http().cachedHostConnectionPoolHttps[Unit](host = "api.github.com")

  private def basicAuth: Option[BasicHttpCredentials] =
    for {
      username <- auth.username
      authtoken <- auth.authtoken
    } yield BasicHttpCredentials(username, authtoken)

  private def headers: Seq[HttpHeader] =
    basicAuth.map(Authorization(_)).to(Seq)

  private def unmarshall(response: HttpResponse): Future[Seq[Repository]] =
    Unmarshal(response).to[Seq[Repository]]

  private def throttle[T, K](source: Source[(T, Unit), K]) =
    basicAuth match {
      case None    => source.throttle(60, 1 hour)
      case Some(_) => source.throttle(5000, 1 hour)
    }

  def source: Source[Repository, NotUsed] =
    throttle {
      Source
        .repeat(
          HttpRequest(uri = "/repositories")
            .withHeaders(headers) -> ()
        )
    }.via(pool).mapAsync(1) {
        case (Success(response), _) =>
          unmarshall(response)
        case (Failure(e), _) =>
          logger.error("Error on request", e)
          throw e
      }.mapConcat(identity)

}
