package io.mwielocha.githubsync.model

object Label extends LongTypedId[Label]

case class Label(
  id: Label.Id,
  url: String,
  name: String
)
