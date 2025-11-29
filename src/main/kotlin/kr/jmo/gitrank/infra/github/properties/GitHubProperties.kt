package kr.jmo.gitrank.infra.github.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "github")
data class GitHubProperties(
    val clientId: String,
    val clientSecret: String,
)
