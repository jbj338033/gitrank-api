package kr.gitrank.api.infra.github.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GitHubRepoDto(
    val id: Long,
    val name: String,
    @JsonProperty("full_name")
    val fullName: String,
    val description: String?,
    val language: String?,
    @JsonProperty("stargazers_count")
    val stargazersCount: Int = 0,
    @JsonProperty("forks_count")
    val forksCount: Int = 0,
    val fork: Boolean = false,
    val owner: GitHubRepoOwnerDto
)

data class GitHubRepoOwnerDto(
    val id: Long,
    val login: String
)
