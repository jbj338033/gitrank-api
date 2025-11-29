package kr.jmo.gitrank.domain.user.presentation.request

import jakarta.validation.constraints.NotNull

data class UpdateVisibilityRequest(
    @field:NotNull(message = "visible is required")
    val visible: Boolean,
)
