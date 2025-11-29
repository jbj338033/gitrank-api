package kr.jmo.gitrank.global.security.jwt.validator

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.jmo.gitrank.domain.auth.domain.error.AuthError
import kr.jmo.gitrank.global.error.BusinessException
import kr.jmo.gitrank.global.security.jwt.enums.JwtType
import kr.jmo.gitrank.global.security.jwt.properties.JwtProperties
import org.springframework.stereotype.Component

@Component
class JwtValidator(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey by lazy { Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray()) }

    fun validate(token: String, expectedType: JwtType) {
        val claims = try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthError.EXPIRED_TOKEN)
        } catch (e: JwtException) {
            throw BusinessException(AuthError.INVALID_TOKEN)
        }

        if (claims["type"] != expectedType.name) {
            throw BusinessException(AuthError.INVALID_TOKEN)
        }
    }
}
