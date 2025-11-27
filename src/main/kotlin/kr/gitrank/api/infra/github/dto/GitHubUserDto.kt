package kr.gitrank.api.infra.github.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GitHubUserDto(
    val id: Long,
    val login: String,
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    val name: String?,
    val followers: Int = 0,
    @JsonProperty("public_repos")
    val publicRepos: Int = 0
)

data class GitHubAccessTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String,
    val scope: String?
)
