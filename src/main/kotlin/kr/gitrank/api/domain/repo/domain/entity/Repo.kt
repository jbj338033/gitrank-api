package kr.gitrank.api.domain.repo.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import kr.gitrank.api.domain.user.domain.entity.User
import kr.gitrank.api.global.jpa.common.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "repos")
class Repo(
    @Column(unique = true, nullable = false)
    val githubRepoId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var fullName: String,

    var description: String? = null,

    var language: String? = null,

    var stars: Int = 0,

    var forks: Int = 0,

    var registered: Boolean = false,

    var lastSyncedAt: LocalDateTime? = null
) : BaseEntity() {

    fun updateRegister(registered: Boolean) {
        this.registered = registered
    }

    fun updateStats(stars: Int, forks: Int) {
        this.stars = stars
        this.forks = forks
        this.lastSyncedAt = LocalDateTime.now()
    }

    fun updateInfo(name: String, fullName: String, description: String?, language: String?) {
        this.name = name
        this.fullName = fullName
        this.description = description
        this.language = language
    }
}
