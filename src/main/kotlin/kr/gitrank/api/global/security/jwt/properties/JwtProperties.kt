package kr.gitrank.api.global.security.jwt.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessExpiry: Long,
    val refreshExpiry: Long
)
