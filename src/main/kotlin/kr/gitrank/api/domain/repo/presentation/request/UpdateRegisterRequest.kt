package kr.gitrank.api.domain.repo.presentation.request

import jakarta.validation.constraints.NotNull

data class UpdateRegisterRequest(
    @field:NotNull(message = "isRegistered is required")
    val isRegistered: Boolean
)
