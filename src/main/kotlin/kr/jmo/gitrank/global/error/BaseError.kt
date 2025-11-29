package kr.jmo.gitrank.global.error

import org.springframework.http.HttpStatus

interface BaseError {
    val status: HttpStatus
    val message: String
}
