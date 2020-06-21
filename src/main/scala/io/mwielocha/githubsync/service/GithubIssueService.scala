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

class GithubIssueService(
  private val api: GithubRemoteService
) extends LazyLogging {

  import api.{ actorSystem, fastRate }
  import actorSystem.dispatcher

  private [service] def baseUri(repository: Repository) =
    Uri(s"/repos/${repository.owner.login}/${repository.name}/issues")

  private [service] def baseQuery(repository: Repository) =
    baseUri(repository).withQuery(Uri.Query("labels" -> "good first issue", "per_page" -> "100"))

  val unfold: Uri => UnfoldAsync[Seq[Issue]] = api.unfold(_)(Nil)

  def source(repository: Repository): Source[Issue, NotUsed] =
   Source
     .unfoldAsync[Uri, Seq[Issue]](baseQuery(repository))(unfold)
     .throttle(fastRate, 1 hour, 1, ThrottleMode.Shaping)
      .mapConcat(identity)

}
