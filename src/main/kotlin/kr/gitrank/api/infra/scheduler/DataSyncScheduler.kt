package kr.gitrank.api.infra.scheduler

import kr.gitrank.api.domain.ranking.application.RankingService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DataSyncScheduler(
    private val rankingService: RankingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 * * * *")
    fun refreshRankings() = runCatching {
        rankingService.refreshUserRankings()
        rankingService.refreshRepoRankings()
    }.onFailure { log.error("Failed to refresh rankings", it) }
}
