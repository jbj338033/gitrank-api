package kr.jmo.gitrank.global.error

class BusinessException(
    val error: BaseError,
) : RuntimeException(error.message)
