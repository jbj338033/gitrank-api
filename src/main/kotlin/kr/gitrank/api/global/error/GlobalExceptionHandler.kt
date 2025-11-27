package kr.gitrank.api.global.error

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        log.warn("BusinessException: {}", e.message)
        return ResponseEntity
            .status(e.error.status)
            .body(ErrorResponse.of(e.error))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        log.warn("Validation failed: {}", e.message)
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse.of(
                    code = "COMMON_INVALID_INPUT_VALUE",
                    status = 400,
                    message = message.ifEmpty { CommonError.INVALID_INPUT_VALUE.message }
                )
            )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        log.warn("Type mismatch: {}", e.message)
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(CommonError.INVALID_INPUT_VALUE))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unexpected error", e)
        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse.of(CommonError.INTERNAL_SERVER_ERROR))
    }
}
