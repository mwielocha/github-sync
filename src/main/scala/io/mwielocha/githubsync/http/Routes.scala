package io.mwielocha.githubsync.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scala.concurrent.Future
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.mwielocha.githubsync.model.Repository
import io.mwielocha.githubsync.model.User
import io.mwielocha.githubsync.persistence.RepositoryStore
import scala.annotation.meta.param

class Routes(repositoryStore: RepositoryStore) extends ErrorAccumulatingCirceSupport {

  private val limit = parameter("offset" ? 0)
  private val offset = parameter("limit" ? 50)

  def apply(): Route =
    path("challenge") {
      (get & offset & limit) { (offset, limit) =>
        complete(
          repositoryStore.findAll(offset, limit)
        )
      }
    }

}
