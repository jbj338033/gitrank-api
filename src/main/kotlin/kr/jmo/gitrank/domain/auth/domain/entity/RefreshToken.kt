package kr.jmo.gitrank.domain.auth.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kr.jmo.gitrank.global.jpa.common.BaseEntity
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Column(nullable = false)
    val userId: UUID,

    @Column(nullable = false, length = 512)
    val token: String,

    @Column(nullable = false)
    val expiresAt: LocalDateTime,
) : BaseEntity() {
    fun isExpired() = LocalDateTime.now().isAfter(expiresAt)
}
