package kr.gitrank.api.domain.ranking.application

import kr.gitrank.api.domain.ranking.presentation.response.RepoRankingResponse
import kr.gitrank.api.domain.ranking.presentation.response.UserRankingResponse
import kr.gitrank.api.global.response.CursorRequest
import kr.gitrank.api.global.response.PageResponse
import kr.gitrank.api.infra.redis.RankingCacheRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RankingService(
    private val rankingCacheRepository: RankingCacheRepository
) {

    fun getUserRankings(
        sort: String,
        period: String,
        language: String?,
        cursorRequest: CursorRequest
    ): PageResponse<UserRankingResponse> {
        val rankings = rankingCacheRepository.getUserRankings(
            sort = sort,
            period = period,
            cursor = cursorRequest.cursor,
            limit = cursorRequest.getEffectiveLimit()
        )

        return PageResponse.of(rankings, cursorRequest.limit)
    }

    fun getRepoRankings(
        sort: String,
        language: String?,
        cursorRequest: CursorRequest
    ): PageResponse<RepoRankingResponse> {
        val rankings = rankingCacheRepository.getRepoRankings(
            sort = sort,
            language = language,
            cursor = cursorRequest.cursor,
            limit = cursorRequest.getEffectiveLimit()
        )

        return PageResponse.of(rankings, cursorRequest.limit)
    }

    fun refreshUserRankings() {
        rankingCacheRepository.refreshUserRankings()
    }

    fun refreshRepoRankings() {
        rankingCacheRepository.refreshRepoRankings()
    }
}
