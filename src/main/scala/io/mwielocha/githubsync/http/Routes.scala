package io.mwielocha.githubsync.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.concurrent.Future
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport

class Routes extends ErrorAccumulatingCirceSupport {

  def apply(): Route =
    path("challenge") {
      get {
        complete(Future.successful(List.empty[Int]))
      }
    }

}
