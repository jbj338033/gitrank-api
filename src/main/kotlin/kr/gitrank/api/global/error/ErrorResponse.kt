package kr.gitrank.api.global.error

import java.time.LocalDateTime

data class ErrorResponse(
    val code: String,
    val status: Int,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun of(error: BaseError): ErrorResponse {
            return ErrorResponse(
                code = error.javaClass.simpleName.replace("Error", "").uppercase() + "_" +
                       (error as Enum<*>).name,
                status = error.status.value(),
                message = error.message
            )
        }

        fun of(code: String, status: Int, message: String): ErrorResponse {
            return ErrorResponse(
                code = code,
                status = status,
                message = message
            )
        }
    }
}
