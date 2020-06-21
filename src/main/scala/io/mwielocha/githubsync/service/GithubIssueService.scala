package io.mwielocha.githubsync.service

import com.typesafe.scalalogging.LazyLogging
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.Uri
import io.mwielocha.githubsync.model.Issue
import io.mwielocha.githubsync.model.Repository
import akka.NotUsed
import akka.stream.ThrottleMode
import scala.concurrent.duration._
import akka.stream.impl.UnfoldAsync
import cats.syntax.option._

class GithubIssueService(
  private val api: GithubRemoteService
) extends LazyLogging {

  import api.{ actorSystem, fastRate }
  import actorSystem.dispatcher

  private[service] def baseUri(repository: Repository) =
    Uri(s"/repos/${repository.owner.login}/${repository.name}/issues")

  private[service] def baseQuery(repository: Repository) =
    baseUri(repository).withQuery(Uri.Query("labels" -> "good first issue", "per_page" -> "5"))

  private def unfold(uri: Option[Uri], getEtag: GetEtag): UnfoldAsync[Seq[Issue]] = api.unfold(uri, getEtag, Nil)

  def source(repository: Repository, getEtag: GetEtag): Source[Resource[Seq[Issue]], NotUsed] =
    Source
      .unfoldAsync[Option[Uri], Resource[Seq[Issue]]](baseQuery(repository).some)(unfold(_, getEtag))
      .throttle(fastRate, 1 hour, 1, ThrottleMode.Shaping)

}
