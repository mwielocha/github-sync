package io.mwielocha.githubsync.model

import java.time.Instant

object Issue extends LongTypedId[Issue]

case class Issue(
  id: Issue.Id,
  url: String,
  user: User,
  title: String,
  createdAt: Instant,
  updatedAt: Instant
)
