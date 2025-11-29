package kr.jmo.gitrank.domain.auth.presentation.request

import jakarta.validation.constraints.NotBlank

data class LogoutRequest(
    @field:NotBlank(message = "refreshToken is required")
    val refreshToken: String,
)
