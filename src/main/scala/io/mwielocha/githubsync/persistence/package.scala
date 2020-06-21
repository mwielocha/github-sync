package io.mwielocha.githubsync

import io.circe.syntax._
import io.circe.{ Decoder, Encoder, Json }
import shapeless.tag
import shapeless.tag.@@

import scala.reflect.ClassTag
import akka.http.scaladsl.model.Uri

package object persistence {

  final val api = io.mwielocha.githubsync.db.PostgresProfile.api

  import api._

  implicit def typedLongType[T]: BaseColumnType[Long @@ T] =
    MappedColumnType.base[Long @@ T, Long](identity, id => tag[T][Long](id))

  def mapJson[E : Decoder : Encoder : ClassTag]: BaseColumnType[E] =
    MappedColumnType.base[E, Json](
      e => e.asJson,
      s =>
        s.as[E] match {
          case Left(e)     => throw e
          case Right(json) => json
        }
    )

  implicit val uriType: BaseColumnType[Uri] =
    MappedColumnType.base[Uri, String](_.toString, Uri(_))

}
