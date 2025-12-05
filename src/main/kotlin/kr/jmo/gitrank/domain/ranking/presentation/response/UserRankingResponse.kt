package kr.jmo.gitrank.domain.ranking.presentation.response

import kr.jmo.gitrank.domain.user.domain.entity.User
import java.util.UUID

data class UserRankingResponse(
    val id: UUID,
    val username: String,
    val name: String?,
    val avatarUrl: String?,
    val bio: String?,
    val commits: Int,
    val stars: Int,
    val followers: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val rank: Int,
) {
    constructor(user: User, period: String, rank: Int) : this(
        id = user.id,
        username = user.username,
        name = user.name,
        avatarUrl = user.avatarUrl,
        bio = user.bio,
        commits =
            when (period) {
                "yearly" -> user.yearlyCommits
                "monthly" -> user.monthlyCommits
                "weekly" -> user.weeklyCommits
                "daily" -> user.dailyCommits
                else -> user.commits
            },
        stars = user.stars,
        followers = user.followers,
        currentStreak = user.currentStreak,
        longestStreak = user.longestStreak,
        rank = rank,
    )
}
