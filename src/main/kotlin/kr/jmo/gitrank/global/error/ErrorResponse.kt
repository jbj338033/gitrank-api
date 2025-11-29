package kr.jmo.gitrank.global.error

import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val status: Int,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    constructor(error: BaseError) : this(
        code = (error as Enum<*>).name,
        status = error.status.value(),
        message = error.message,
    )
}
