package kr.gitrank.api.global.security.oauth2.github

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth2.github")
data class GitHubOAuth2Properties(
    val clientId: String,
    val clientSecret: String
)
