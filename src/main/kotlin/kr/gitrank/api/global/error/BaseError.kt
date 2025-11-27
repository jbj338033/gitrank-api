package kr.gitrank.api.global.error

import org.springframework.http.HttpStatus

interface BaseError {
    val status: HttpStatus
    val message: String
}
