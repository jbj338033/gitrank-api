package kr.jmo.gitrank.infra.github.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.jmo.gitrank.domain.user.domain.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class GitHubSyncScheduler(
    private val userRepository: UserRepository,
    private val gitHubSyncService: GitHubSyncService,
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "0 10 20 * * *")
    fun syncAll() {
        logger.info { "Starting scheduled GitHub sync" }

        val userIds = userRepository.findAllIdsWithRefreshToken()

        userIds.forEach { userId ->
            Thread.startVirtualThread {
                runCatching { gitHubSyncService.sync(userId) }
                    .onFailure { logger.error(it) { "Failed to sync user: $userId" } }
            }
        }

        logger.info { "Scheduled sync initiated for ${userIds.size} users" }
    }
}
