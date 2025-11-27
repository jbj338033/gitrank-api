package kr.gitrank.api.global.error

class BusinessException(
    val error: BaseError,
    val args: Array<Any> = emptyArray()
) : RuntimeException(error.message)
