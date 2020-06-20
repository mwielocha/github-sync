package io.mwielocha.githubsync.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.concurrent.Future
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.mwielocha.githubsync.model.Repository
import io.mwielocha.githubsync.model.User

class Routes extends ErrorAccumulatingCirceSupport {

  def apply(): Route =
    path("challenge") {
      get {
        complete(
          Future.successful(
            List(
              Repository(
                Repository.Id(1L),
                User(
                  User.Id(1L),
                  "John"
                ),
                "Random repo",
                "http://google.com",
                "Full name random repo",
                "Just a repo"
              )
            )
          )
        )
      }
    }

}
