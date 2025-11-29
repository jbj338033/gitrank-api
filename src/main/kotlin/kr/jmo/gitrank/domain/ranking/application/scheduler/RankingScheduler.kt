package kr.jmo.gitrank.domain.ranking.application.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.jmo.gitrank.domain.ranking.application.service.RankingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RankingScheduler(
    private val rankingService: RankingService,
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(cron = "0 0 * * * *")
    fun refreshRankings() =
        runCatching {
            rankingService.refreshUserRankings()
            rankingService.refreshRepoRankings()
        }.onFailure { logger.error(it) { "Failed to refresh rankings" } }
}
