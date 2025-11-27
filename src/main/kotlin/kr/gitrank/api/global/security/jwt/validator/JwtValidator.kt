package kr.gitrank.api.global.security.jwt.validator

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.gitrank.api.global.security.jwt.enums.JwtType
import kr.gitrank.api.global.security.jwt.properties.JwtProperties
import org.springframework.stereotype.Component
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtValidator(
    private val jwtProperties: JwtProperties
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun validateToken(token: String, expectedType: JwtType): Boolean {
        return try {
            val claims = parseClaims(token)
            val type = claims["type"] as? String
            type == expectedType.name
        } catch (e: JwtException) {
            false
        }
    }

    fun isExpired(token: String): Boolean {
        return try {
            parseClaims(token)
            false
        } catch (e: ExpiredJwtException) {
            true
        } catch (e: JwtException) {
            true
        }
    }

    fun getUserId(token: String): UUID {
        val claims = parseClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun getUsername(token: String): String {
        val claims = parseClaims(token)
        return claims["username"] as String
    }

    fun getTokenType(token: String): JwtType {
        val claims = parseClaims(token)
        val type = claims["type"] as String
        return JwtType.valueOf(type)
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
