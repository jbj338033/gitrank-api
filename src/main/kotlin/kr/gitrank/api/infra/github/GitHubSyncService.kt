package kr.gitrank.api.infra.github

import kr.gitrank.api.domain.repo.application.RepoService
import kr.gitrank.api.domain.user.application.UserService
import kr.gitrank.api.domain.user.domain.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GitHubSyncService(
    private val gitHubClient: GitHubClient,
    private val userService: UserService,
    private val repoService: RepoService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sync(user: User, token: String) = runCatching {
        syncStats(user, token)
        syncRepos(user, token)
    }.onFailure { log.error("Sync failed: ${user.username}", it) }

    private fun syncStats(user: User, token: String) {
        val githubUser = gitHubClient.fetchUser(token) ?: return
        val repos = gitHubClient.fetchRepos(token, user.username)
        val totalStars = repos.filterNot { it.fork }.sumOf { it.stargazersCount }
        val totalContributions = gitHubClient.fetchTotalContributions(token)

        userService.updateStats(user.id, totalContributions, totalStars, githubUser.followers)
    }

    private fun syncRepos(user: User, token: String) {
        gitHubClient.fetchRepos(token, user.username)
            .filterNot { it.fork }
            .forEach { repo ->
                repoService.upsertRepo(
                    user, repo.id, repo.name, repo.fullName,
                    repo.description, repo.language, repo.stargazersCount, repo.forksCount
                )
            }
    }
}
