package io.mwielocha.githubsync.persistence

import api._

import shapeless.{ HList, ::, HNil }
import slickless._
import cats.syntax.option._
import scala.concurrent.Future
import akka.stream.scaladsl.Sink
import akka.NotUsed
import akka.Done
import com.typesafe.scalalogging.LazyLogging
import io.mwielocha.githubsync.model.Repository
import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.model.Uri
import scala.concurrent.ExecutionContext
import cats.implicits._
import cats.syntax.option._
import cats.data.OptionT

class Etags(tag: Tag) extends Table[(Uri, EntityTag)](tag, "etags") {

  def uri = column[Uri]("uri", O.PrimaryKey)
  def etag = column[String]("etag")
  def weak = column[Boolean]("weak")

  override def * =
    uri :: etag :: weak :: HNil <> ({
      case uri ::
            etag ::
            weak ::
            HNil =>
        uri -> EntityTag(etag, weak)
    }, { etag: (Uri, EntityTag) =>
      Some(
        etag._1 ::
          etag._2.tag ::
          etag._2.weak ::
          HNil
      )
    })
}

class EtagStore(db: Database) extends LazyLogging {

  object etags extends TableQuery(new Etags(_))

  def insertOrUpdate(etag: (Uri, EntityTag))(implicit ec: ExecutionContext): Future[(Uri, EntityTag)] =
    db.run {
      etags.insertOrUpdate(etag)
    } map (_ => etag)

  def find(uri: Uri)(implicit ec: ExecutionContext): Future[Option[EntityTag]] =
    (for {
      (_, etag) <- OptionT {
        db.run {
          etags.filter(_.uri === uri).result.headOption
        }
      }
    } yield etag).value

  def sink(implicit ec: ExecutionContext): Sink[(Uri, EntityTag), Future[Done]] =
    Sink.foreachAsync[(Uri, EntityTag)](1) { etag =>
      for {
        _ <- insertOrUpdate(etag)
      } yield logger.debug("Stored: {}", etag)
    }
}
