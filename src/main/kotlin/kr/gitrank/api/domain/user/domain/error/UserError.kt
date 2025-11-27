package kr.gitrank.api.domain.user.domain.error

import kr.gitrank.api.global.error.BaseError
import org.springframework.http.HttpStatus

enum class UserError(
    override val status: HttpStatus,
    override val message: String
) : BaseError {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "User already exists")
}
