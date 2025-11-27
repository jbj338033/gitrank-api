package kr.gitrank.api.domain.auth.presentation.request

import jakarta.validation.constraints.NotBlank

data class RefreshRequest(
    @field:NotBlank(message = "refreshToken is required")
    val refreshToken: String
)
