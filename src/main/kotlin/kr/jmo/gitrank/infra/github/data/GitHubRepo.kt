package kr.jmo.gitrank.infra.github.data

import com.fasterxml.jackson.annotation.JsonProperty

data class GitHubRepo(
    val id: Long,
    val name: String,
    val description: String?,
    val language: String?,
    val fork: Boolean = false,
    val owner: GitHubRepoOwner,

    @JsonProperty("full_name")
    val fullName: String,

    @JsonProperty("stargazers_count")
    val stars: Int = 0,

    @JsonProperty("forks_count")
    val forks: Int = 0,
)

data class GitHubRepoOwner(
    val id: Long,
    val login: String,
)
