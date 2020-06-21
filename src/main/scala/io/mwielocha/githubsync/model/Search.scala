package io.mwielocha.githubsync.model

object Search {
  def empty[T] = Search[T](Nil, 0)
}

case class Search[T](
  items: Seq[T],
  totalCount: Long
)
