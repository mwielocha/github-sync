package io.mwielocha.githubsync.service

import akka.http.scaladsl.model.headers.EntityTag
import akka.http.scaladsl.model.Uri

case class Resource[T](data: T, uri: Uri, etag: Option[EntityTag] = None)
