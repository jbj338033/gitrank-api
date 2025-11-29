package kr.jmo.gitrank.domain.user.domain.repository

import kr.jmo.gitrank.domain.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByGithubId(githubId: Long): User?

    fun findByIdAndDeletedAtIsNull(id: UUID): User?

    @Query("SELECT u.id FROM User u WHERE u.githubRefreshToken IS NOT NULL AND u.deletedAt IS NULL")
    fun findAllIdsWithRefreshToken(): List<UUID>
}
