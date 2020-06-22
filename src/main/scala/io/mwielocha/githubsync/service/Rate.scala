package io.mwielocha.githubsync.service

import scala.concurrent.duration.FiniteDuration

case class Rate(
  amount: Int,
  interval: FiniteDuration
)
