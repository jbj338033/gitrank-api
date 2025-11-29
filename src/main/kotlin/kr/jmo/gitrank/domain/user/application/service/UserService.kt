package kr.jmo.gitrank.domain.user.application.service

import kr.jmo.gitrank.domain.user.domain.error.UserError
import kr.jmo.gitrank.domain.user.domain.repository.UserRepository
import kr.jmo.gitrank.domain.user.presentation.response.UserResponse
import kr.jmo.gitrank.global.error.BusinessException
import kr.jmo.gitrank.global.security.holder.SecurityHolder
import kr.jmo.gitrank.infra.github.service.GitHubSyncService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val securityHolder: SecurityHolder,
    private val gitHubSyncService: GitHubSyncService,
) {
    fun getMe(): UserResponse {
        val user = userRepository.findByIdAndDeletedAtIsNull(securityHolder.userId())
            ?: throw BusinessException(UserError.USER_NOT_FOUND)

        return UserResponse(user)
    }

    @Transactional
    fun updateVisibility(visible: Boolean) {
        val user = userRepository.findByIdAndDeletedAtIsNull(securityHolder.userId())
            ?: throw BusinessException(UserError.USER_NOT_FOUND)

        user.updateVisibility(visible)
    }

    @Transactional
    fun deleteMe() {
        val user = userRepository.findByIdAndDeletedAtIsNull(securityHolder.userId())
            ?: throw BusinessException(UserError.USER_NOT_FOUND)

        user.delete()
    }

    fun sync() {
        gitHubSyncService.sync(securityHolder.userId())
    }
}
