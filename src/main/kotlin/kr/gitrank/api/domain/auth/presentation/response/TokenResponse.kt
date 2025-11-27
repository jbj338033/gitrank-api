package kr.gitrank.api.domain.auth.presentation.response

import kr.gitrank.api.domain.user.presentation.response.UserResponse

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse? = null
)
