package io.mwielocha.githubsync

import akka.http.scaladsl.model.Uri
import scala.concurrent.Future
import scala.util.Try
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.EntityTag

package object service {

  type UnfoldAsync[T] = Future[Option[(Option[Uri], Resource[T])]]

  type Call = (Uri, Option[EntityTag]) => Future[Try[HttpResponse]]

  type GetEtag = Uri => Future[Option[EntityTag]]

}
