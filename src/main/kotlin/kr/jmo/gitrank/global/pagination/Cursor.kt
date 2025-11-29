package kr.jmo.gitrank.global.pagination

import java.util.UUID

data class Cursor(
    val cursor: UUID? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    fun getEffectiveLimit() = (limit + 1).coerceAtMost(MAX_LIMIT + 1)

    companion object {
        const val DEFAULT_LIMIT = 30
        const val MAX_LIMIT = 100
    }
}
