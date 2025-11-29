package kr.jmo.gitrank.infra.github.data

import com.fasterxml.jackson.annotation.JsonProperty

data class GitHubUser(
    val id: Long,
    val login: String,
    val name: String?,
    val followers: Int = 0,

    @JsonProperty("avatar_url")
    val avatarUrl: String?,

    @JsonProperty("public_repos")
    val publicRepos: Int = 0,
)

data class GitHubAccessToken(
    val scope: String?,

    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("refresh_token")
    val refreshToken: String?,

    @JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Int?,
)
