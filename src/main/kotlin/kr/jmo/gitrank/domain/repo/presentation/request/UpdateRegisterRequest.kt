package kr.jmo.gitrank.domain.repo.presentation.request

import jakarta.validation.constraints.NotNull

data class UpdateRegisterRequest(
    @field:NotNull(message = "registered is required")
    val registered: Boolean,
)
