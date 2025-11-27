package kr.gitrank.api.infra.scheduler

import kr.gitrank.api.domain.ranking.application.RankingService
import kr.gitrank.api.domain.user.domain.repository.UserRepository
import kr.gitrank.api.infra.github.GitHubClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DataSyncScheduler(
    private val userRepository: UserRepository,
    private val gitHubClient: GitHubClient,
    private val rankingService: RankingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Refresh rankings every hour
    @Scheduled(cron = "0 0 * * * *")
    fun refreshRankings() {
        log.info("Starting scheduled ranking refresh...")
        try {
            rankingService.refreshUserRankings()
            rankingService.refreshRepoRankings()
            log.info("Scheduled ranking refresh completed")
        } catch (e: Exception) {
            log.error("Failed to refresh rankings", e)
        }
    }

    // Sync all users daily at midnight UTC
    @Scheduled(cron = "0 0 0 * * *")
    fun syncAllUsers() {
        log.info("Starting daily user sync...")
        try {
            val users = userRepository.findAll()
                .filter { !it.isDeleted && it.isVisible }

            log.info("Found ${users.size} users to sync")

            // In production, this would sync user data from GitHub
            // For now, we'll just log
            users.forEach { user ->
                log.debug("Would sync user: ${user.username}")
            }

            log.info("Daily user sync completed")
        } catch (e: Exception) {
            log.error("Failed to sync users", e)
        }
    }
}
