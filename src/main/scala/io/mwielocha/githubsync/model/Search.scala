package io.mwielocha.githubsync.model

case class Search[T](
  items: Seq[T],
  totalCount: Long,
)
