package io.mwielocha.githubsync.model

import shapeless.tag
import shapeless.tag.@@

trait LongTypedId[Entity] {

  type Id = Long @@ Entity

  object Id {
    def apply(id: Long): Id =
      tag[Entity][Long](id)
  }
}
