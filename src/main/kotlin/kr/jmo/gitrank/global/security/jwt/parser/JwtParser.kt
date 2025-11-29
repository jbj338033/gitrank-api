package kr.jmo.gitrank.global.security.jwt.parser

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.jmo.gitrank.global.security.jwt.properties.JwtProperties
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JwtParser(
    private val jwtProperties: JwtProperties,
) {
    private val secretKey by lazy { Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray()) }

    fun getUserId(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return UUID.fromString(claims.subject)
    }
}
