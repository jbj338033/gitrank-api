package kr.gitrank.api.infra.redis

import kr.gitrank.api.domain.ranking.presentation.response.RepoRankingResponse
import kr.gitrank.api.domain.ranking.presentation.response.UserRankingResponse
import kr.gitrank.api.domain.repo.domain.repository.RepoRepository
import kr.gitrank.api.domain.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.UUID

@Repository
class RankingCacheRepository(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val userRepository: UserRepository,
    private val repoRepository: RepoRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val USER_RANKING_KEY_PREFIX = "ranking:users:"
        private const val REPO_RANKING_KEY_PREFIX = "ranking:repos:"
        private val CACHE_TTL = Duration.ofHours(1)
    }

    fun getUserRankings(
        sort: String,
        period: String,
        cursor: UUID?,
        limit: Int
    ): List<UserRankingResponse> {
        val key = buildUserRankingKey(sort, period)

        // Try to get from cache first
        val cached = getCachedUserRankings(key, cursor, limit)
        if (cached.isNotEmpty()) {
            return cached
        }

        // Fallback to database
        return fetchUserRankingsFromDb(sort, period, cursor, limit)
    }

    fun getRepoRankings(
        sort: String,
        language: String?,
        cursor: UUID?,
        limit: Int
    ): List<RepoRankingResponse> {
        val key = buildRepoRankingKey(sort, language)

        // Try to get from cache first
        val cached = getCachedRepoRankings(key, cursor, limit)
        if (cached.isNotEmpty()) {
            return cached
        }

        // Fallback to database
        return fetchRepoRankingsFromDb(sort, language, cursor, limit)
    }

    fun refreshUserRankings() {
        log.info("Refreshing user rankings cache...")

        val sortTypes = listOf("commits", "stars", "followers")
        val periods = listOf("all", "yearly", "monthly", "weekly")

        for (sort in sortTypes) {
            for (period in periods) {
                try {
                    val key = buildUserRankingKey(sort, period)
                    val rankings = computeUserRankings(sort, period)
                    cacheUserRankings(key, rankings)
                } catch (e: Exception) {
                    log.error("Failed to refresh user rankings for sort=$sort, period=$period", e)
                }
            }
        }

        log.info("User rankings cache refreshed")
    }

    fun refreshRepoRankings() {
        log.info("Refreshing repo rankings cache...")

        val sortTypes = listOf("stars", "forks")
        val languages = listOf(null, "JavaScript", "TypeScript", "Python", "Java", "Kotlin",
            "Go", "Rust", "C", "C++", "C#", "Ruby", "PHP", "Swift", "Dart", "Shell")

        for (sort in sortTypes) {
            for (language in languages) {
                try {
                    val key = buildRepoRankingKey(sort, language)
                    val rankings = computeRepoRankings(sort, language)
                    cacheRepoRankings(key, rankings)
                } catch (e: Exception) {
                    log.error("Failed to refresh repo rankings for sort=$sort, language=$language", e)
                }
            }
        }

        log.info("Repo rankings cache refreshed")
    }

    private fun buildUserRankingKey(sort: String, period: String): String {
        return if (period == "all") {
            "$USER_RANKING_KEY_PREFIX$sort"
        } else {
            "$USER_RANKING_KEY_PREFIX$sort:$period"
        }
    }

    private fun buildRepoRankingKey(sort: String, language: String?): String {
        return if (language == null) {
            "$REPO_RANKING_KEY_PREFIX$sort"
        } else {
            "$REPO_RANKING_KEY_PREFIX$sort:lang:${language.lowercase()}"
        }
    }

    private fun getCachedUserRankings(key: String, cursor: UUID?, limit: Int): List<UserRankingResponse> {
        // Simplified implementation - in production, use sorted sets with ZRANGE
        return emptyList()
    }

    private fun getCachedRepoRankings(key: String, cursor: UUID?, limit: Int): List<RepoRankingResponse> {
        // Simplified implementation - in production, use sorted sets with ZRANGE
        return emptyList()
    }

    private fun fetchUserRankingsFromDb(
        sort: String,
        period: String,
        cursor: UUID?,
        limit: Int
    ): List<UserRankingResponse> {
        val users = userRepository.findAll()
            .filter { !it.isDeleted && it.isVisible }
            .sortedByDescending { user ->
                when (sort) {
                    "commits" -> user.totalCommits
                    "stars" -> user.totalStars
                    "followers" -> user.totalFollowers
                    else -> user.totalCommits
                }
            }

        val startIndex = if (cursor != null) {
            users.indexOfFirst { it.id == cursor }.let { if (it >= 0) it + 1 else 0 }
        } else {
            0
        }

        return users
            .drop(startIndex)
            .take(limit)
            .mapIndexed { index, user ->
                UserRankingResponse.from(user, startIndex + index + 1)
            }
    }

    private fun fetchRepoRankingsFromDb(
        sort: String,
        language: String?,
        cursor: UUID?,
        limit: Int
    ): List<RepoRankingResponse> {
        val repos = repoRepository.findByIsRegisteredTrueAndDeletedAtIsNull()
            .filter { language == null || it.language.equals(language, ignoreCase = true) }
            .sortedByDescending { repo ->
                when (sort) {
                    "stars" -> repo.stars
                    "forks" -> repo.forks
                    else -> repo.stars
                }
            }

        val startIndex = if (cursor != null) {
            repos.indexOfFirst { it.id == cursor }.let { if (it >= 0) it + 1 else 0 }
        } else {
            0
        }

        return repos
            .drop(startIndex)
            .take(limit)
            .mapIndexed { index, repo ->
                RepoRankingResponse.from(repo, startIndex + index + 1)
            }
    }

    private fun computeUserRankings(sort: String, period: String): List<UserRankingResponse> {
        return fetchUserRankingsFromDb(sort, period, null, 1000)
    }

    private fun computeRepoRankings(sort: String, language: String?): List<RepoRankingResponse> {
        return fetchRepoRankingsFromDb(sort, language, null, 1000)
    }

    private fun cacheUserRankings(key: String, rankings: List<UserRankingResponse>) {
        try {
            redisTemplate.opsForValue().set(key, rankings, CACHE_TTL)
        } catch (e: Exception) {
            log.error("Failed to cache user rankings for key: $key", e)
        }
    }

    private fun cacheRepoRankings(key: String, rankings: List<RepoRankingResponse>) {
        try {
            redisTemplate.opsForValue().set(key, rankings, CACHE_TTL)
        } catch (e: Exception) {
            log.error("Failed to cache repo rankings for key: $key", e)
        }
    }
}
