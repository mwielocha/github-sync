package io.mwielocha.githubsync.model

import shapeless.tag
import shapeless.tag.@@

trait LongTypedId[T] {

  type Id = Long @@ T

  object Id {
    def apply(id: Long): Id =
      tag[T][Long](id)
  }
}
