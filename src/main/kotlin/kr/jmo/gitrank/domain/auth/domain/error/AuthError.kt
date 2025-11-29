package kr.jmo.gitrank.domain.auth.domain.error

import kr.jmo.gitrank.global.error.BaseError
import org.springframework.http.HttpStatus

enum class AuthError(
    override val status: HttpStatus,
    override val message: String,
) : BaseError {
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Token has expired"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    GITHUB_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "GitHub authentication failed"),
    INVALID_AUTHORIZATION_CODE(HttpStatus.BAD_REQUEST, "Invalid authorization code"),
}
