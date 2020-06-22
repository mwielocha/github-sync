package io.mwielocha.githubsync.service

import io.mwielocha.githubsync.model.Repository
import io.mwielocha.githubsync.model.User
import io.mwielocha.githubsync.model.Issue
import java.time.Instant

trait Fixtures {

  val repository = Repository(
      Repository.Id(956452),
      User(User.Id(7874557), "LambdaHack"),
      "LambdaHack",
      "https://github.com/LambdaHack/LambdaHack",
      "LambdaHack/LambdaHack",
      Some("Haskell game engine library for roguelike dungeon crawlers."),
      None
  )

  val issue = Issue(
    id = Issue.Id(610667939),
    url = "https://api.github.com/repos/LambdaHack/LambdaHack/issues/200",
    user = User(User.Id(281893), "Mikolaj"),
    title = "Wrap the pathfinding/projecting epsilons in a newtype",
    createdAt = Instant.now,
    updatedAt = Instant.now,
    labels = Nil
  )

}
