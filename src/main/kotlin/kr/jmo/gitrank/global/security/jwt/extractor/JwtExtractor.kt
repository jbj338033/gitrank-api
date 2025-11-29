package kr.jmo.gitrank.global.security.jwt.extractor

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class JwtExtractor {
    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }

    fun extract(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null

        return if (authorizationHeader.startsWith(BEARER_PREFIX)) {
            authorizationHeader.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
