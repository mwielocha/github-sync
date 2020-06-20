package io.mwielocha.githubsync.persistence

import api._

import scala.concurrent.ExecutionContext.Implicits.global
import io.mwielocha.githubsync.model.Repository
import shapeless.{ HList, ::, HNil }
import slickless._
import cats.syntax.option._
import scala.concurrent.Future
import akka.stream.scaladsl.Sink
import akka.NotUsed
import akka.Done
import com.typesafe.scalalogging.LazyLogging

class Repositories(tag: Tag) extends Table[Repository](tag, "repositories") {

  private implicit val mapRepository = mapJson[Repository]

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

class RepositoryStore(db: Database) extends LazyLogging {

  object repositories extends TableQuery(new Repositories(_))

  def insertOrUpdate(repository: Repository): Future[Repository] =
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

  def sink: Sink[Repository, Future[Done]] =
    Sink.foreachAsync[Repository](1) { repository =>
      for {
        _ <- insertOrUpdate(repository)
      } yield logger.debug("Stored: {}", repository)
    }

}
