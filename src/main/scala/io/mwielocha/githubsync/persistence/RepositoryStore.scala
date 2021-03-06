package io.mwielocha.githubsync.persistence

import api._

import io.mwielocha.githubsync.model.{ Repository, Issue }
import shapeless.{ ::, HNil }
import slickless._
import cats.syntax.option._
import scala.concurrent.Future
import akka.stream.scaladsl.Sink
import akka.Done
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.ExecutionContext

class Repositories(tag: Tag) extends Table[Repository](tag, "repositories") {
  implicit val mapRepository = mapJson[Repository]

  def id = column[Repository.Id]("id", O.PrimaryKey)
  def repository = column[Repository]("repository")

  override def * =
    id :: repository :: HNil <> ({
      case _ ::
            repository ::
            HNil =>
        repository
    }, { repository: Repository =>
      (repository.id ::
        repository ::
        HNil).some
    })
}

class RepositoryStore(db: Database, issueStore: IssueStore) extends LazyLogging {

  import issueStore.issues

  implicit val mapIssue = mapJson[Issue]

  object repositories extends TableQuery(new Repositories(_))

  def insertOrUpdate(repository: Repository)(implicit ec: ExecutionContext): Future[Repository] =
    db.run {
      repositories.insertOrUpdate(repository)
    } map (_ => repository)

  def find(id: Repository.Id): Future[Option[Repository]] =
    db.run {
      repositories.filter(_.id === id).result.headOption
    }

  def findAll(limit: Int, offset: Int): Future[Seq[Repository]] =
    db.run {
      repositories
        .drop(offset)
        .take(limit)
        .result
    }

  def findAllWithLatestIssue(limit: Int, offset: Int): Future[Seq[(Repository, Option[Issue])]] =
    db.run {
      val latestIssues = issues
        .join(
          for {
            (repositoryId, issues) <- issues.groupBy(_.repositoryId)
          } yield repositoryId -> issues.map(_.createdAt).max
        ).on {
          case (issue, (repositoryId, createdAt)) =>
            issue.repositoryId === repositoryId &&
              issue.createdAt === createdAt
        }.map(_._1)

      val reposWithLatestIssue = for {
        (repository, issue) <- repositories.joinLeft(latestIssues).on {
          case (repository, issue) =>
            repository.id === issue.repositoryId
        }
      } yield repository -> issue.map(_.issue)

      reposWithLatestIssue.drop(offset).take(limit).result
    }

  def sink(implicit ec: ExecutionContext): Sink[Repository, Future[Done]] =
    Sink.foreachAsync[Repository](1) { repository =>
      for {
        _ <- insertOrUpdate(repository)
      } yield logger.debug("Stored: {}", repository)
    }

}
