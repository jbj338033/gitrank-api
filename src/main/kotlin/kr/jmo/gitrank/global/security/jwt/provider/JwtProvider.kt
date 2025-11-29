package kr.jmo.gitrank.global.security.jwt.provider

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.jmo.gitrank.global.security.jwt.enums.JwtType
import kr.jmo.gitrank.global.security.jwt.properties.JwtProperties
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey by lazy { Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray()) }

    fun createAccessToken(userId: UUID) = createToken(userId, JwtType.ACCESS, jwtProperties.accessExpiry)

    fun createRefreshToken(userId: UUID) = createToken(userId, JwtType.REFRESH, jwtProperties.refreshExpiry)

    private fun createToken(userId: UUID, type: JwtType, expiry: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }
}
