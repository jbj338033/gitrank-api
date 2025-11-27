package kr.gitrank.api.global.security.oauth2.github

import org.springframework.context.annotation.Configuration

@Configuration
class GitHubOAuth2Config(
    private val gitHubOAuth2Properties: GitHubOAuth2Properties
) {
    fun getClientId(): String = gitHubOAuth2Properties.clientId

    fun getClientSecret(): String = gitHubOAuth2Properties.clientSecret
}
