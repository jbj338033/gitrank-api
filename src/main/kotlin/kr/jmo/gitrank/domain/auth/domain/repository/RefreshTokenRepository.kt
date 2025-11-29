package kr.jmo.gitrank.domain.auth.domain.repository

import kr.jmo.gitrank.domain.auth.domain.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByTokenAndDeletedAtIsNull(token: String): RefreshToken?
}
