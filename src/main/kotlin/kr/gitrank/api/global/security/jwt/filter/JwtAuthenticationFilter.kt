package kr.gitrank.api.global.security.jwt.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.gitrank.api.global.security.jwt.enums.JwtType
import kr.gitrank.api.global.security.jwt.extractor.JwtExtractor
import kr.gitrank.api.global.security.jwt.validator.JwtValidator
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtExtractor: JwtExtractor,
    private val jwtValidator: JwtValidator
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = jwtExtractor.extract(request)

        if (token != null && jwtValidator.validateToken(token, JwtType.ACCESS)) {
            val userId = jwtValidator.getUserId(token)
            val username = jwtValidator.getUsername(token)

            val authentication = UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER"))
            )
            authentication.details = mapOf("username" to username)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}
