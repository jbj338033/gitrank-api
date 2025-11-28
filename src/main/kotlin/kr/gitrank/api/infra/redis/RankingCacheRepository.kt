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

    fun getUserRankings(sort: String, period: String, cursor: UUID?, limit: Int): List<UserRankingResponse> =
        fetchUserRankings(sort, cursor, limit)

    fun getRepoRankings(sort: String, language: String?, cursor: UUID?, limit: Int): List<RepoRankingResponse> =
        fetchRepoRankings(sort, language, cursor, limit)

    fun refreshUserRankings() {
        SORT_TYPES.forEach { sort ->
            PERIODS.forEach { period ->
                runCatching {
                    val key = if (period == "all") "$USER_KEY$sort" else "$USER_KEY$sort:$period"
                    redisTemplate.opsForValue().set(key, fetchUserRankings(sort, null, MAX_CACHE_SIZE), TTL)
                }.onFailure { log.error("Failed to refresh user rankings: $sort/$period", it) }
            }
        }
    }

    fun refreshRepoRankings() {
        listOf("stars", "forks").forEach { sort ->
            LANGUAGES.forEach { lang ->
                runCatching {
                    val key = lang?.let { "$REPO_KEY$sort:lang:${it.lowercase()}" } ?: "$REPO_KEY$sort"
                    redisTemplate.opsForValue().set(key, fetchRepoRankings(sort, lang, null, MAX_CACHE_SIZE), TTL)
                }.onFailure { log.error("Failed to refresh repo rankings: $sort/$lang", it) }
            }
        }
    }

    private fun fetchUserRankings(sort: String, cursor: UUID?, limit: Int): List<UserRankingResponse> {
        val users = userRepository.findAll()
            .filter { !it.isDeleted && it.isVisible }
            .sortedByDescending {
                when (sort) {
                    "stars" -> it.totalStars
                    "followers" -> it.totalFollowers
                    else -> it.totalCommits
                }
            }

        val start = cursor?.let { c -> users.indexOfFirst { it.id == c }.takeIf { it >= 0 }?.plus(1) } ?: 0
        return users.drop(start).take(limit).mapIndexed { i, user -> UserRankingResponse.from(user, start + i + 1) }
    }

    private fun fetchRepoRankings(sort: String, language: String?, cursor: UUID?, limit: Int): List<RepoRankingResponse> {
        val repos = repoRepository.findByIsRegisteredTrueAndDeletedAtIsNull()
            .filter { language == null || it.language.equals(language, ignoreCase = true) }
            .sortedByDescending { if (sort == "forks") it.forks else it.stars }

        val start = cursor?.let { c -> repos.indexOfFirst { it.id == c }.takeIf { it >= 0 }?.plus(1) } ?: 0
        return repos.drop(start).take(limit).mapIndexed { i, repo -> RepoRankingResponse.from(repo, start + i + 1) }
    }

    companion object {
        private const val USER_KEY = "ranking:users:"
        private const val REPO_KEY = "ranking:repos:"
        private const val MAX_CACHE_SIZE = 1000
        private val TTL = Duration.ofHours(1)
        private val SORT_TYPES = listOf("commits", "stars", "followers")
        private val PERIODS = listOf("all", "yearly", "monthly", "weekly")
        private val LANGUAGES = listOf(
            null, "JavaScript", "TypeScript", "Python", "Java", "Kotlin",
            "Go", "Rust", "C", "C++", "C#", "Ruby", "PHP", "Swift", "Dart", "Shell"
        )
    }
}
