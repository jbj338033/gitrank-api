package kr.gitrank.api.domain.user.presentation.response

import kr.gitrank.api.domain.user.domain.entity.User
import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val githubId: Long,
    val username: String,
    val avatarUrl: String?,
    val totalCommits: Int,
    val totalStars: Int,
    val totalFollowers: Int,
    val visible: Boolean,
    val lastSyncedAt: LocalDateTime?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id,
                githubId = user.githubId,
                username = user.username,
                avatarUrl = user.avatarUrl,
                totalCommits = user.totalCommits,
                totalStars = user.totalStars,
                totalFollowers = user.totalFollowers,
                visible = user.visible,
                lastSyncedAt = user.lastSyncedAt,
                createdAt = user.createdAt
            )
        }
    }
}
