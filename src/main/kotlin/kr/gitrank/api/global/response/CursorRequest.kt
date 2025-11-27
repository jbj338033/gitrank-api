package kr.gitrank.api.global.response

import java.util.UUID

data class CursorRequest(
    val cursor: UUID? = null,
    val limit: Int = DEFAULT_LIMIT
) {
    fun getEffectiveLimit(): Int = (limit + 1).coerceAtMost(MAX_LIMIT + 1)

    companion object {
        const val DEFAULT_LIMIT = 30
        const val MAX_LIMIT = 100
    }
}
