package kr.gitrank.api.domain.auth.domain.repository

import kr.gitrank.api.domain.auth.domain.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {

    fun findByToken(token: String): RefreshToken?

    fun findByUserIdAndDeletedAtIsNull(userId: UUID): List<RefreshToken>

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.deletedAt = :now WHERE rt.user.id = :userId")
    fun invalidateAllByUserId(userId: UUID, now: LocalDateTime = LocalDateTime.now())

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    fun deleteExpiredTokens(now: LocalDateTime = LocalDateTime.now())
}
