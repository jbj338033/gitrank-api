package kr.gitrank.api.domain.ranking.application

import kr.gitrank.api.domain.ranking.presentation.response.RepoRankingResponse
import kr.gitrank.api.domain.ranking.presentation.response.UserRankingResponse
import kr.gitrank.api.global.response.CursorRequest
import kr.gitrank.api.global.response.PageResponse
import kr.gitrank.api.infra.redis.RankingCacheRepository
import org.springframework.stereotype.Service

@Service
class RankingService(
    private val rankingCacheRepository: RankingCacheRepository
) {

    fun getUserRankings(sort: String, period: String, language: String?, request: CursorRequest): PageResponse<UserRankingResponse> =
        PageResponse.of(rankingCacheRepository.getUserRankings(sort, period, request.cursor, request.getEffectiveLimit()), request.limit)

    fun getRepoRankings(sort: String, language: String?, request: CursorRequest): PageResponse<RepoRankingResponse> =
        PageResponse.of(rankingCacheRepository.getRepoRankings(sort, language, request.cursor, request.getEffectiveLimit()), request.limit)

    fun refreshUserRankings() = rankingCacheRepository.refreshUserRankings()

    fun refreshRepoRankings() = rankingCacheRepository.refreshRepoRankings()
}
