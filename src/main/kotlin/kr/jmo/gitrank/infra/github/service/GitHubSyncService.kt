package kr.jmo.gitrank.infra.github.service

import kr.jmo.gitrank.domain.repo.domain.entity.Repo
import kr.jmo.gitrank.domain.repo.domain.repository.RepoRepository
import kr.jmo.gitrank.domain.user.domain.entity.User
import kr.jmo.gitrank.domain.user.domain.repository.UserRepository
import kr.jmo.gitrank.infra.github.client.GitHubClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class GitHubSyncService(
    private val gitHubClient: GitHubClient,
    private val userRepository: UserRepository,
    private val repoRepository: RepoRepository,
) {
    @Transactional
    fun sync(userId: UUID) {
        val user = userRepository.findByIdOrNull(userId) ?: return
        var token = user.githubAccessToken ?: return

        val expiresAt = user.githubTokenExpiresAt
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            val refreshToken = user.githubRefreshToken ?: return
            val newToken = gitHubClient.refreshAccessToken(refreshToken) ?: return

            user.updateGitHubTokens(
                newToken.accessToken,
                newToken.refreshToken,
                newToken.refreshTokenExpiresIn,
            )

            token = newToken.accessToken
        }

        syncRepos(user, token)
        syncStats(user, token)
    }

    @Transactional
    fun syncRepos(
        userId: UUID,
        token: String,
    ) {
        val user = userRepository.findByIdOrNull(userId) ?: return

        syncRepos(user, token)
    }

    @Transactional
    fun syncStats(
        userId: UUID,
        token: String,
    ) {
        val user = userRepository.findByIdOrNull(userId) ?: return

        syncStats(user, token)
    }

    private fun syncRepos(
        user: User,
        token: String,
    ) {
        val repos = gitHubClient.fetchRepos(token).filterNot { it.fork }

        repos.forEach { repo ->
            repoRepository.findByGithubRepoId(repo.id)?.apply {
                if (isDeleted) activate()
                updateInfo(repo.name, repo.fullName, repo.description, repo.language)
                updateStats(repo.stars, repo.forks)
            } ?: repoRepository.save(
                Repo(repo.id, user, repo.name, repo.fullName, repo.description, repo.language, repo.stars, repo.forks),
            )
        }
    }

    private fun syncStats(
        user: User,
        token: String,
    ) {
        val githubUser = gitHubClient.fetchUser(token) ?: return
        val repos = gitHubClient.fetchRepos(token).filterNot { it.fork }
        val contributions = gitHubClient.fetchContributions(token)

        user.updateStats(
            contributions.total,
            contributions.yearly,
            contributions.monthly,
            contributions.weekly,
            contributions.daily,
            repos.sumOf { it.stars },
            githubUser.followers,
        )
    }
}
