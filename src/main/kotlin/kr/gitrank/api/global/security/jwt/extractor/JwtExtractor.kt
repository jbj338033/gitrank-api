package kr.gitrank.api.global.security.jwt.extractor

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class JwtExtractor {

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    fun extract(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader(AUTHORIZATION_HEADER) ?: return null

        return if (authorizationHeader.startsWith(BEARER_PREFIX)) {
            authorizationHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
