package kr.jmo.gitrank.domain.ranking.presentation.response

import kr.jmo.gitrank.domain.user.domain.entity.User
import java.util.UUID

data class UserRankingResponse(
    val id: UUID,
    val username: String,
    val avatarUrl: String?,
    val commits: Int,
    val stars: Int,
    val followers: Int,
    val rank: Int,
) {
    constructor(user: User, period: String, rank: Int) : this(
        id = user.id,
        username = user.username,
        avatarUrl = user.avatarUrl,
        commits = when (period) {
            "yearly" -> user.yearlyCommits
            "monthly" -> user.monthlyCommits
            "weekly" -> user.weeklyCommits
            else -> user.commits
        },
        stars = user.stars,
        followers = user.followers,
        rank = rank,
    )
}
