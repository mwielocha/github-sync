package io.mwielocha.githubsync.model

object Repository extends LongTypedId[Repository]

case class Repository(
  id: Repository.Id,
  owner: User,
  name: String,
  htmlUrl: String,
  fullName: String,
  description: Option[String],
  issue: Option[Issue]
)
