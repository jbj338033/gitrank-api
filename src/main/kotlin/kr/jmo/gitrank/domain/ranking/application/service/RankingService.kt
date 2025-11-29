package kr.jmo.gitrank.domain.ranking.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.jmo.gitrank.domain.ranking.presentation.response.RepoRankingResponse
import kr.jmo.gitrank.domain.ranking.presentation.response.UserRankingResponse
import kr.jmo.gitrank.domain.repo.domain.repository.RepoRepository
import kr.jmo.gitrank.domain.user.domain.repository.UserRepository
import kr.jmo.gitrank.global.pagination.Cursor
import kr.jmo.gitrank.global.pagination.PageResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RankingService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val userRepository: UserRepository,
    private val repoRepository: RepoRepository,
) {
    private val logger = KotlinLogging.logger {}

    fun getUserRankings(
        sort: String,
        period: String,
        cursor: Cursor,
    ): PageResponse<UserRankingResponse> {
        val rankings = fetchUserRankings(sort, period, cursor)

        return PageResponse.of(rankings, cursor.limit)
    }

    fun getRepoRankings(
        sort: String,
        cursor: Cursor,
    ): PageResponse<RepoRankingResponse> {
        val rankings = fetchRepoRankings(sort, cursor)

        return PageResponse.of(rankings, cursor.limit)
    }

    fun refreshUserRankings() {
        USER_SORT_TYPES.forEach { sort ->
            USER_PERIODS.forEach { period ->
                runCatching {
                    val key = if (period == "all") "$USER_KEY$sort" else "$USER_KEY$sort:$period"
                    redisTemplate.opsForValue().set(key, fetchUserRankings(sort, period, null), TTL)
                }.onFailure {
                    logger.error(it) { "Failed to refresh user rankings: $sort/$period" }
                }
            }
        }
    }

    fun refreshRepoRankings() {
        REPO_SORT_TYPES.forEach { sort ->
            runCatching {
                redisTemplate.opsForValue().set("$REPO_KEY$sort", fetchRepoRankings(sort, null), TTL)
            }.onFailure {
                logger.error(it) { "Failed to refresh repo rankings: $sort" }
            }
        }
    }

    private fun fetchUserRankings(
        sort: String,
        period: String,
        cursor: Cursor?,
    ): List<UserRankingResponse> {
        val limit = cursor?.getEffectiveLimit() ?: MAX_CACHE_SIZE
        val users =
            userRepository
                .findAll()
                .filter { !it.isDeleted && it.visible }
                .sortedByDescending {
                    when (sort) {
                        "stars" -> it.stars
                        "followers" -> it.followers
                        else ->
                            when (period) {
                                "yearly" -> it.yearlyCommits
                                "monthly" -> it.monthlyCommits
                                "weekly" -> it.weeklyCommits
                                else -> it.commits
                            }
                    }
                }

        val start =
            cursor
                ?.cursor
                ?.let { c -> users.indexOfFirst { it.id == c }.takeIf { it >= 0 }?.plus(1) }
                ?: 0

        return users
            .drop(start)
            .take(limit)
            .mapIndexed { i, user -> UserRankingResponse(user, period, start + i + 1) }
    }

    private fun fetchRepoRankings(
        sort: String,
        cursor: Cursor?,
    ): List<RepoRankingResponse> {
        val limit = cursor?.getEffectiveLimit() ?: MAX_CACHE_SIZE
        val repos =
            repoRepository
                .findByRegisteredTrueAndDeletedAtIsNull()
                .sortedByDescending { if (sort == "forks") it.forks else it.stars }

        val start =
            cursor
                ?.cursor
                ?.let { c -> repos.indexOfFirst { it.id == c }.takeIf { it >= 0 }?.plus(1) }
                ?: 0

        return repos
            .drop(start)
            .take(limit)
            .mapIndexed { i, repo -> RepoRankingResponse(repo, start + i + 1) }
    }

    companion object {
        private const val USER_KEY = "ranking:users:"
        private const val REPO_KEY = "ranking:repos:"
        private const val MAX_CACHE_SIZE = 1000
        private val TTL = Duration.ofHours(1)
        private val USER_SORT_TYPES = listOf("commits", "stars", "followers")
        private val USER_PERIODS = listOf("all", "yearly", "monthly", "weekly")
        private val REPO_SORT_TYPES = listOf("stars", "forks")
    }
}
