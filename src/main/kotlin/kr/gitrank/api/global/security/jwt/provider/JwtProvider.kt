package kr.gitrank.api.global.security.jwt.provider

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.gitrank.api.global.security.jwt.enums.JwtType
import kr.gitrank.api.global.security.jwt.properties.JwtProperties
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun createAccessToken(userId: UUID, username: String): String {
        return createToken(userId, username, JwtType.ACCESS, jwtProperties.accessExpiry)
    }

    fun createRefreshToken(userId: UUID, username: String): String {
        return createToken(userId, username, JwtType.REFRESH, jwtProperties.refreshExpiry)
    }

    private fun createToken(userId: UUID, username: String, type: JwtType, expiry: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry * 1000)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("type", type.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }
}
