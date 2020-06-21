package io.mwielocha.githubsync.service

import com.typesafe.scalalogging.LazyLogging
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.Uri
import io.mwielocha.githubsync.model.Search
import io.mwielocha.githubsync.model.Repository
import akka.NotUsed
import akka.stream.ThrottleMode
import scala.concurrent.duration._
import akka.stream.impl.UnfoldAsync

class GithubRepositoryService(
  private val api: GithubRemoteService
) extends LazyLogging {

  import api.{ actorSystem, slowRate }
  import actorSystem.dispatcher

  private [service] val baseUri = Uri("/search/repositories")

  private [service] val baseQuery = baseUri.withQuery(
    Uri.Query(
      "q" -> "good-first-issues:>1 language:haskell",
      "sort" -> "help-wanted-issues",
      "order" -> "desc",
      "per_page" -> "50"
    )
  )

  val unfold: Uri => UnfoldAsync[Search[Repository]] = api.unfold(_)(Search.empty)

  def source: Source[Repository, NotUsed] =
   Source
     .unfoldAsync[Uri, Search[Repository]](baseQuery)(unfold)
      .map(_.items)
     .throttle(slowRate, 1 minute, 1, ThrottleMode.Shaping)
      .mapConcat(identity)

}
