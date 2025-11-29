package kr.jmo.gitrank.global.security.jwt.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.jmo.gitrank.global.error.BusinessException
import kr.jmo.gitrank.global.error.ErrorResponse
import kr.jmo.gitrank.global.security.jwt.enums.JwtType
import kr.jmo.gitrank.global.security.jwt.extractor.JwtExtractor
import kr.jmo.gitrank.global.security.jwt.parser.JwtParser
import kr.jmo.gitrank.global.security.jwt.validator.JwtValidator
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

@Component
class JwtAuthenticationFilter(
    private val jwtExtractor: JwtExtractor,
    private val jwtValidator: JwtValidator,
    private val jwtParser: JwtParser,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = jwtExtractor.extract(request)

        if (token != null) {
            try {
                jwtValidator.validate(token, JwtType.ACCESS)

                val userId = jwtParser.getUserId(token)
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)

                SecurityContextHolder.getContext().authentication = auth
            } catch (e: BusinessException) {
                response.sendError(e)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletResponse.sendError(e: BusinessException) {
        status = e.error.status.value()
        contentType = MediaType.APPLICATION_JSON_VALUE
        characterEncoding = "UTF-8"
        writer.write(objectMapper.writeValueAsString(ErrorResponse(e.error)))
    }
}
