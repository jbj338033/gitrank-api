package kr.jmo.gitrank.domain.user.domain.error

import kr.jmo.gitrank.global.error.BaseError
import org.springframework.http.HttpStatus

enum class UserError(
    override val status: HttpStatus,
    override val message: String,
) : BaseError {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
}
