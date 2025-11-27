package kr.gitrank.api.domain.ranking.presentation.response

import kr.gitrank.api.domain.user.domain.entity.User
import java.util.UUID

data class UserRankingResponse(
    val id: UUID,
    val username: String,
    val avatarUrl: String?,
    val totalCommits: Int,
    val totalStars: Int,
    val totalFollowers: Int,
    val rank: Int
) {
    companion object {
        fun from(user: User, rank: Int): UserRankingResponse {
            return UserRankingResponse(
                id = user.id,
                username = user.username,
                avatarUrl = user.avatarUrl,
                totalCommits = user.totalCommits,
                totalStars = user.totalStars,
                totalFollowers = user.totalFollowers,
                rank = rank
            )
        }
    }
}
