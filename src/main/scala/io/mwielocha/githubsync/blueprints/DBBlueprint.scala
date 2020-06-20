package io.mwielocha.githubsync.blueprints

import factorio._

import io.mwielocha.githubsync.db.PostgresProfile.api._

@blueprint
trait DBBlueprint {

  @provides
  def database: Database =
    Database.forConfig("db")

}
