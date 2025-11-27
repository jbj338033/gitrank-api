package kr.gitrank.api.domain.user.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.gitrank.api.global.jpa.common.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    val githubId: Long,

    @Column(nullable = false)
    var username: String,

    var avatarUrl: String? = null,

    var totalCommits: Int = 0,

    var totalStars: Int = 0,

    var totalFollowers: Int = 0,

    var isVisible: Boolean = true,

    var lastSyncedAt: LocalDateTime? = null
) : BaseEntity() {

    fun updateVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
    }

    fun updateProfile(username: String, avatarUrl: String?) {
        this.username = username
        this.avatarUrl = avatarUrl
    }

    fun updateStats(totalCommits: Int, totalStars: Int, totalFollowers: Int) {
        this.totalCommits = totalCommits
        this.totalStars = totalStars
        this.totalFollowers = totalFollowers
        this.lastSyncedAt = LocalDateTime.now()
    }
}
