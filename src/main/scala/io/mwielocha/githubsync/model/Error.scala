package io.mwielocha.githubsync.model

case class Error(
  message: String,
  documentationUrl: Option[String]
) extends Exception(message)
