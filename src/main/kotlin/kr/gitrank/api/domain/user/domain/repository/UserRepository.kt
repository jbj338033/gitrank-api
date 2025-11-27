package kr.gitrank.api.domain.user.domain.repository

import kr.gitrank.api.domain.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {

    fun findByGithubId(githubId: Long): User?

    fun findByIdAndDeletedAtIsNull(id: UUID): User?

    fun findByUsernameAndDeletedAtIsNull(username: String): User?

    fun existsByGithubId(githubId: Long): Boolean
}
