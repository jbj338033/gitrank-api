package kr.jmo.gitrank.domain.repo.presentation.response

import kr.jmo.gitrank.domain.repo.domain.entity.Repo
import java.time.LocalDateTime
import java.util.UUID

data class RepoResponse(
    val id: UUID,
    val githubRepoId: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val registered: Boolean,
    val lastSyncedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    constructor(repo: Repo) : this(
        id = repo.id,
        githubRepoId = repo.githubRepoId,
        name = repo.name,
        fullName = repo.fullName,
        description = repo.description,
        language = repo.language,
        stars = repo.stars,
        forks = repo.forks,
        registered = repo.registered,
        lastSyncedAt = repo.lastSyncedAt,
        createdAt = repo.createdAt,
    )
}
