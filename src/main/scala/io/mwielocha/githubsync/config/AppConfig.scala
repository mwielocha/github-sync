package io.mwielocha.githubsync.config

case class AppConfig(
  host: String,
  port: Int,
  github: GithubAuth
)

case class GithubAuth(
  username: Option[String] = None,
  authtoken: Option[String] = None
)
