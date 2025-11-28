package kr.gitrank.api.domain.auth.domain.entity

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
@Table(name = "refresh_tokens")
class RefreshToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 512)
    val token: String,

    @Column(nullable = false)
    val expiresAt: LocalDateTime
) : BaseEntity() {

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiresAt)
    }
}
