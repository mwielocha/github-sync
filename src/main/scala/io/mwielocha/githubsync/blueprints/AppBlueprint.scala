package io.mwielocha.githubsync.blueprints

import io.mwielocha.githubsync.config.AppConfig

import factorio._

class AppBlueprint(config: AppConfig) extends AkkaBlueprint with HttpBlueprint with DBBlueprint {

  @provides
  def getConfig: AppConfig =
    config

}
