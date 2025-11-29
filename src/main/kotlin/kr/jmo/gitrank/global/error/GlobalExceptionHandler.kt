package kr.jmo.gitrank.global.error

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn { "BusinessException: ${e.message}" }

        return ResponseEntity
            .status(e.error.status)
            .body(ErrorResponse(e.error))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn { "Validation failed: ${e.message}" }

        val message =
            e.bindingResult.fieldErrors
                .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }

        return ResponseEntity
            .badRequest()
            .body(
                ErrorResponse(
                    code = "INVALID_INPUT_VALUE",
                    status = 400,
                    message = message.ifEmpty { CommonError.INVALID_INPUT_VALUE.message },
                ),
            )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn { "Type mismatch: ${e.message}" }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(CommonError.INVALID_INPUT_VALUE))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Unexpected error" }

        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse(CommonError.INTERNAL_SERVER_ERROR))
    }
}
