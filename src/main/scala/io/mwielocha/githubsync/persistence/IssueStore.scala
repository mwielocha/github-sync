package io.mwielocha.githubsync.persistence

import api._

import scala.concurrent.ExecutionContext.Implicits.global
import io.mwielocha.githubsync.model.Issue
import shapeless.{ HList, ::, HNil }
import slickless._
import cats.syntax.option._
import scala.concurrent.Future
import akka.stream.scaladsl.Sink
import akka.NotUsed
import akka.Done
import com.typesafe.scalalogging.LazyLogging
import io.mwielocha.githubsync.model.Repository
import java.time.Instant

class Issues(tag: Tag) extends Table[(Repository.Id, Issue)](tag, "issues") {

  implicit val mapIssue = mapJson[Issue]

  def id = column[Issue.Id]("id", O.PrimaryKey)
  def issue = column[Issue]("issue")
  def createdAt = column[Instant]("created_at")
  def repositoryId = column[Repository.Id]("repository_id")

  override def * =
    id :: issue :: createdAt :: repositoryId :: HNil <> ({
      case _ :: 
          issue ::
          _ ::
            repositoryId ::
            HNil =>
        repositoryId -> issue
    }, { issue: (Repository.Id, Issue) =>
      (issue._2.id ::
         issue._2 ::
         issue._2.createdAt ::
        issue._1 ::
        HNil).some
    })
}

class IssueStore(db: Database) extends LazyLogging {

  object issues extends TableQuery(new Issues(_))

  def insertOrUpdate(issue: (Repository.Id, Issue)): Future[(Repository.Id, Issue)] =
    db.run {
      issues.insertOrUpdate(issue)
    } map (_ => issue)

  def find(id: Issue.Id): Future[Option[(Repository.Id, Issue)]] =
    db.run {
      issues.filter(_.id === id).result.headOption
    }

  def findAll(limit: Int, offset: Int): Future[Seq[(Repository.Id, Issue)]] =
    db.run {
      issues
        .drop(offset)
        .take(limit)
        .result
    }

  def sink: Sink[(Repository.Id, Issue), Future[Done]] =
    Sink.foreachAsync[(Repository.Id, Issue)](1) { issue =>
      for {
        _ <- insertOrUpdate(issue)
      } yield logger.debug("Stored: {}", issue)
    }

}
