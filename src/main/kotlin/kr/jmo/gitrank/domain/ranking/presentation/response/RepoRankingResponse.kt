package kr.jmo.gitrank.domain.ranking.presentation.response

import kr.jmo.gitrank.domain.repo.domain.entity.Repo
import java.util.UUID

data class RepoRankingResponse(
    val id: UUID,
    val name: String,
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val owner: Owner,
    val rank: Int,
) {
    data class Owner(
        val username: String,
        val avatarUrl: String?,
    )

    constructor(repo: Repo, rank: Int) : this(
        id = repo.id,
        name = repo.name,
        fullName = repo.fullName,
        description = repo.description,
        language = repo.language,
        stars = repo.stars,
        forks = repo.forks,
        owner = Owner(repo.user.username, repo.user.avatarUrl),
        rank = rank,
    )
}
