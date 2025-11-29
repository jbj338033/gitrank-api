package kr.jmo.gitrank.domain.repo.domain.repository

import kr.jmo.gitrank.domain.repo.domain.entity.Repo
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface RepoRepository : JpaRepository<Repo, UUID> {
    fun findByGithubRepoId(githubRepoId: Long): Repo?

    fun findByIdAndDeletedAtIsNull(id: UUID): Repo?

    @EntityGraph(attributePaths = ["user"])
    fun findByRegisteredTrueAndDeletedAtIsNull(): List<Repo>

    @Query(
        "SELECT r FROM Repo r WHERE r.user.id = :userId AND r.deletedAt IS NULL AND (:query IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY r.stars DESC",
    )
    fun searchByUserIdAndQuery(
        userId: UUID,
        query: String?,
    ): List<Repo>
}
