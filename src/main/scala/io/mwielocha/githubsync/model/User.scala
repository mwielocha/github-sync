package io.mwielocha.githubsync.model

object User extends LongTypedId[User]

case class User(
  id: User.Id,
  login: String
)
