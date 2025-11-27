package kr.gitrank.api.domain.ranking.presentation.response

import kr.gitrank.api.domain.repo.domain.entity.Repo
import java.util.UUID

data class RepoRankingResponse(
    val id: UUID,
    val name: String,
    val fullName: String,
    val description: String?,
    val language: String?,
    val stars: Int,
    val forks: Int,
    val ownerUsername: String,
    val ownerAvatarUrl: String?,
    val rank: Int
) {
    companion object {
        fun from(repo: Repo, rank: Int): RepoRankingResponse {
            return RepoRankingResponse(
                id = repo.id,
                name = repo.name,
                fullName = repo.fullName,
                description = repo.description,
                language = repo.language,
                stars = repo.stars,
                forks = repo.forks,
                ownerUsername = repo.user.username,
                ownerAvatarUrl = repo.user.avatarUrl,
                rank = rank
            )
        }
    }
}
