package kr.gitrank.api.domain.user.application

import kr.gitrank.api.domain.user.domain.entity.User
import kr.gitrank.api.domain.user.domain.error.UserError
import kr.gitrank.api.domain.user.domain.repository.UserRepository
import kr.gitrank.api.global.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {

    fun getUserById(id: UUID): User {
        return userRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw BusinessException(UserError.USER_NOT_FOUND)
    }

    fun getUserByGithubId(githubId: Long): User? {
        return userRepository.findByGithubId(githubId)
    }

    @Transactional
    fun createUser(githubId: Long, username: String, avatarUrl: String?): User {
        val user = User(
            githubId = githubId,
            username = username,
            avatarUrl = avatarUrl
        )
        return userRepository.save(user)
    }

    @Transactional
    fun getOrCreateUser(githubId: Long, username: String, avatarUrl: String?): User {
        return userRepository.findByGithubId(githubId)?.also { user ->
            if (user.isDeleted) {
                user.activate()
            }
            user.updateProfile(username, avatarUrl)
        } ?: createUser(githubId, username, avatarUrl)
    }

    @Transactional
    fun updateVisibility(userId: UUID, isVisible: Boolean): User {
        val user = getUserById(userId)
        user.updateVisibility(isVisible)
        return user
    }

    @Transactional
    fun deleteUser(userId: UUID) {
        val user = getUserById(userId)
        user.delete()
    }

    @Transactional
    fun updateUserStats(userId: UUID, totalCommits: Int, totalStars: Int, totalFollowers: Int): User {
        val user = getUserById(userId)
        user.updateStats(totalCommits, totalStars, totalFollowers)
        return user
    }
}
