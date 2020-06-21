package io.mwielocha.githubsync

import akka.http.scaladsl.model.Uri
import scala.concurrent.Future
import scala.util.Try
import akka.http.scaladsl.model.HttpResponse

package object service {

  type UnfoldAsync[T] = Future[Option[(Option[Uri], T)]]

  type Call = Uri => Future[Try[HttpResponse]]

}
