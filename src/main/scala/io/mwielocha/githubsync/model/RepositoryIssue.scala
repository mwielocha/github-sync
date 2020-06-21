package io.mwielocha.githubsync.model

case class RepositoryIssue(
  issue: Issue,
  repositoryId: Repository.Id
) {
  val id: Issue.Id = issue.id
}
