package kr.gitrank.api.global.security.holder

import kr.gitrank.api.global.error.BusinessException
import kr.gitrank.api.global.error.CommonError
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SecurityHolder {

    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw BusinessException(CommonError.UNAUTHORIZED)

        return authentication.principal as? UUID
            ?: throw BusinessException(CommonError.UNAUTHORIZED)
    }

    fun getCurrentUsername(): String {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw BusinessException(CommonError.UNAUTHORIZED)

        @Suppress("UNCHECKED_CAST")
        val details = authentication.details as? Map<String, Any>
            ?: throw BusinessException(CommonError.UNAUTHORIZED)

        return details["username"] as? String
            ?: throw BusinessException(CommonError.UNAUTHORIZED)
    }

    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal is UUID
    }
}
