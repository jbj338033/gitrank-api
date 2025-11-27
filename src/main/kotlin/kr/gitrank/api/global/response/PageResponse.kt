package kr.gitrank.api.global.response

data class PageResponse<T>(
    val content: List<T>,
    val hasNext: Boolean
) {
    companion object {
        fun <T> of(content: List<T>, limit: Int): PageResponse<T> {
            val hasNext = content.size > limit
            val actualContent = if (hasNext) content.dropLast(1) else content
            return PageResponse(actualContent, hasNext)
        }
    }
}
