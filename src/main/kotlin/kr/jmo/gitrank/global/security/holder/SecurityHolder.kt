package kr.jmo.gitrank.global.security.holder

import kr.jmo.gitrank.global.error.BusinessException
import kr.jmo.gitrank.global.error.CommonError
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SecurityHolder {
    fun userId(): UUID {
        val principal = SecurityContextHolder.getContext().authentication?.principal

        return principal as? UUID ?: throw BusinessException(CommonError.UNAUTHORIZED)
    }
}
