package kr.gitrank.api.domain.user.application

import kr.gitrank.api.domain.user.domain.entity.User
import kr.gitrank.api.domain.user.domain.error.UserError
import kr.gitrank.api.domain.user.domain.repository.UserRepository
import kr.gitrank.api.global.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getUser(id: UUID): User =
        userRepository.findByIdAndDeletedAtIsNull(id) ?: throw BusinessException(UserError.USER_NOT_FOUND)

    @Transactional
    fun upsertUser(githubId: Long, username: String, avatarUrl: String?): User =
        userRepository.findByGithubId(githubId)?.apply {
            if (isDeleted) activate()
            updateProfile(username, avatarUrl)
        } ?: userRepository.save(User(githubId, username, avatarUrl))

    @Transactional
    fun updateVisibility(userId: UUID, visible: Boolean): User =
        getUser(userId).apply { updateVisibility(visible) }

    @Transactional
    fun delete(userId: UUID) = getUser(userId).delete()

    @Transactional
    fun updateStats(userId: UUID, commits: Int, stars: Int, followers: Int): User =
        getUser(userId).apply { updateStats(commits, stars, followers) }
}
