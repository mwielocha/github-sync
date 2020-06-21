package io.mwielocha.githubsync.persistence

import api._

import scala.concurrent.ExecutionContext.Implicits.global
import io.mwielocha.githubsync.model.{ RepositoryIssue, Issue }
import shapeless.{ HList, ::, HNil }
import slickless._
import cats.syntax.option._
import scala.concurrent.Future
import akka.stream.scaladsl.Sink
import akka.NotUsed
import akka.Done
import com.typesafe.scalalogging.LazyLogging
import io.mwielocha.githubsync.model.Repository

class Issues(tag: Tag) extends Table[RepositoryIssue](tag, "issues") {

  private implicit val mapIssue = mapJson[Issue]

  def id = column[Issue.Id]("id", O.PrimaryKey)
  def issue = column[Issue]("issue")
  def repositoryId = column[Repository.Id]("repository_id")

  override def * =
    id :: issue :: repositoryId :: HNil <> ({
      case _ ::
            issue ::
            repositoryId ::
            HNil =>
        RepositoryIssue(issue, repositoryId)
    }, {
      case RepositoryIssue(issue, repositoryId) =>
        (issue.id ::
          issue ::
          repositoryId ::
          HNil).some
    }: RepositoryIssue => Option[Issue.Id :: Issue :: Repository.Id :: HNil])
}

class IssueStore(db: Database) extends LazyLogging {

  object issues extends TableQuery(new Issues(_))

  def insertOrUpdate(issue: RepositoryIssue): Future[RepositoryIssue] =
    db.run {
      issues.insertOrUpdate(issue)
    } map (_ => issue)

  def find(id: Issue.Id): Future[Option[RepositoryIssue]] =
    db.run {
      issues.filter(_.id === id).result.headOption
    }

  def findAll(limit: Int, offset: Int): Future[Seq[RepositoryIssue]] =
    db.run {
      issues
        .drop(offset)
        .take(limit)
        .result
    }

  def sink: Sink[RepositoryIssue, Future[Done]] =
    Sink.foreachAsync[RepositoryIssue](1) { issue =>
      for {
        _ <- insertOrUpdate(issue)
      } yield logger.debug("Stored: {}", issue)
    }

}
