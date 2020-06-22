package io.mwielocha.githubsync.service

import com.typesafe.scalalogging.LazyLogging
import akka.stream.scaladsl.Source
import akka.http.scaladsl.model.Uri
import io.mwielocha.githubsync.model.Search
import io.mwielocha.githubsync.model.Repository
import akka.NotUsed
import akka.stream.ThrottleMode
import cats.syntax.option._
import scala.concurrent.Future

class GithubRepositoryService(
  private val api: GithubRemoteService
) extends LazyLogging {

  import api.slowRate

  private[service] val baseUri = Uri("/search/repositories")

  private[service] val baseQuery = baseUri.withQuery(
    Uri.Query(
      "q" -> "good-first-issues:>1 language:haskell",
      "sort" -> "help-wanted-issues",
      "order" -> "desc",
      "per_page" -> "50"
    )
  )

  // /search api doesn't support etags
  private val emptyEtag: GetEtag = _ => Future.successful(None)

  private def unfold(uri: Option[Uri]): UnfoldAsync[Search[Repository]] =
    api.unfold(uri, emptyEtag, Search.empty)

  def source: Source[Resource[Search[Repository]], NotUsed] =
    Source
      .unfoldAsync[Option[Uri], Resource[Search[Repository]]](baseQuery.some)(unfold)
      .throttle(slowRate.amount, slowRate.interval, 1, ThrottleMode.Shaping)

}
