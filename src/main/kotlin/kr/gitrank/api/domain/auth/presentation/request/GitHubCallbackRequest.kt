package kr.gitrank.api.domain.auth.presentation.request

import jakarta.validation.constraints.NotBlank

data class GitHubCallbackRequest(
    @field:NotBlank(message = "code is required")
    val code: String
)
