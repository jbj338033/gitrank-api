package kr.jmo.gitrank.domain.user.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.jmo.gitrank.global.jpa.common.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    val githubId: Long,

    @Column(nullable = false)
    var username: String,
    var avatarUrl: String? = null,

    @Column(length = 512)
    var githubAccessToken: String? = null,

    @Column(length = 512)
    var githubRefreshToken: String? = null,
    var githubTokenExpiresAt: LocalDateTime? = null,

    var commits: Int = 0,
    var yearlyCommits: Int = 0,
    var monthlyCommits: Int = 0,
    var weeklyCommits: Int = 0,
    var stars: Int = 0,
    var followers: Int = 0,
    var visible: Boolean = true,
    var lastSyncedAt: LocalDateTime? = null,
) : BaseEntity() {
    fun updateGitHubTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Int?) {
        this.githubAccessToken = accessToken
        this.githubRefreshToken = refreshToken
        this.githubTokenExpiresAt = expiresInSeconds?.let {
            LocalDateTime.now().plusSeconds(it.toLong())
        }
    }

    fun updateVisibility(visible: Boolean) {
        this.visible = visible
    }

    fun updateProfile(username: String, avatarUrl: String?) {
        this.username = username
        this.avatarUrl = avatarUrl
    }

    fun updateStats(
        commits: Int,
        yearlyCommits: Int,
        monthlyCommits: Int,
        weeklyCommits: Int,
        stars: Int,
        followers: Int,
    ) {
        this.commits = commits
        this.yearlyCommits = yearlyCommits
        this.monthlyCommits = monthlyCommits
        this.weeklyCommits = weeklyCommits
        this.stars = stars
        this.followers = followers
        this.lastSyncedAt = LocalDateTime.now()
    }
}
