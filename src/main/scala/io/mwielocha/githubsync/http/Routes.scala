package io.mwielocha.githubsync.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import io.mwielocha.githubsync.persistence.RepositoryStore
import io.circe.Printer

class Routes(repositoryStore: RepositoryStore) extends ErrorAccumulatingCirceSupport {

  private final implicit val printer = Printer.noSpaces.copy(dropNullValues = true)

  private val limit = parameter("offset" ? 0)
  private val offset = parameter("limit" ? 50)

  def apply(): Route = extractExecutionContext { implicit ec =>
    path("challenge") {
      (get & offset & limit) { (offset, limit) =>
        complete(
          repositoryStore.findAllWithLatestIssue(offset, limit).map {
            _.map {
              case (repository, latestIssue) =>
                repository.copy(latestIssue = latestIssue)
            }
          }
        )
      }
    }
  }
}
