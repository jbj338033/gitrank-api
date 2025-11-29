package kr.jmo.gitrank.domain.user.presentation.response

import kr.jmo.gitrank.domain.user.domain.entity.User
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val githubId: Long,
    val username: String,
    val avatarUrl: String?,
    val visible: Boolean,
) {
    constructor(user: User) : this(
        id = user.id,
        githubId = user.githubId,
        username = user.username,
        avatarUrl = user.avatarUrl,
        visible = user.visible,
    )
}
