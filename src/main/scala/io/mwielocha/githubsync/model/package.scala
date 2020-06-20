package io.mwielocha.githubsync

import io.circe.generic.extras.semiauto._
import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.Encoder
import shapeless.tag
import shapeless.tag.@@
import io.circe.Decoder

package object model {

  private implicit val codecConfig = Configuration.default.withSnakeCaseMemberNames

  implicit def encodeTypeLongId[T]: Encoder[Long @@ T] =
    Encoder.encodeLong.contramap(identity)

  implicit def decodeTypeLongId[T]: Decoder[Long @@ T] =
    Decoder.decodeLong.map(tag[T][Long](_))

  implicit val userCodec: Codec[User] = deriveConfiguredCodec[User]
  implicit val errorCodec: Codec[Error] = deriveConfiguredCodec[Error]
  implicit val repositoryCodec: Codec[Repository] = deriveConfiguredCodec[Repository]

  implicit def searchCodec[T : Codec]: Codec[Search[T]] = deriveConfiguredCodec[Search[T]]

}
