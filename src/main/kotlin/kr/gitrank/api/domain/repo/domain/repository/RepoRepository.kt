package kr.gitrank.api.domain.repo.domain.repository

import kr.gitrank.api.domain.repo.domain.entity.Repo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RepoRepository : JpaRepository<Repo, UUID> {

    fun findByGithubRepoId(githubRepoId: Long): Repo?

    fun findByIdAndDeletedAtIsNull(id: UUID): Repo?

    fun findByUserIdAndDeletedAtIsNull(userId: UUID): List<Repo>

    fun findByIsRegisteredTrueAndDeletedAtIsNull(): List<Repo>

    @Query("""
        SELECT r FROM Repo r
        WHERE r.user.id = :userId
        AND r.deletedAt IS NULL
        ORDER BY r.stars DESC
    """)
    fun findByUserIdOrderByStarsDesc(userId: UUID): List<Repo>

    @Query("""
        SELECT r FROM Repo r
        WHERE r.user.id = :userId
        AND r.deletedAt IS NULL
        AND (:query IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY r.stars DESC
    """)
    fun searchByUserIdAndQuery(userId: UUID, query: String?): List<Repo>

    @Query("""
        SELECT r FROM Repo r
        WHERE r.isRegistered = true
        AND r.deletedAt IS NULL
        AND (:language IS NULL OR r.language = :language)
        ORDER BY r.stars DESC
    """)
    fun findRegisteredReposByLanguage(language: String?): List<Repo>
}
