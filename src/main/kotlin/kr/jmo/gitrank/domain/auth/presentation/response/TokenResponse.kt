package kr.jmo.gitrank.domain.auth.presentation.response

import kr.jmo.gitrank.domain.user.presentation.response.UserResponse

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse? = null,
)
