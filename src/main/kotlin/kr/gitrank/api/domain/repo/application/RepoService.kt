package kr.gitrank.api.domain.repo.application

import kr.gitrank.api.domain.repo.domain.entity.Repo
import kr.gitrank.api.domain.repo.domain.error.RepoError
import kr.gitrank.api.domain.repo.domain.repository.RepoRepository
import kr.gitrank.api.domain.user.domain.entity.User
import kr.gitrank.api.global.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RepoService(
    private val repoRepository: RepoRepository
) {

    fun getRepo(id: UUID): Repo =
        repoRepository.findByIdAndDeletedAtIsNull(id) ?: throw BusinessException(RepoError.REPO_NOT_FOUND)

    fun getReposByUserId(userId: UUID): List<Repo> =
        repoRepository.findByUserIdOrderByStarsDesc(userId)

    fun getRegisteredRepos(language: String?): List<Repo> =
        repoRepository.findRegisteredReposByLanguage(language)

    @Transactional
    fun upsertRepo(
        user: User,
        githubRepoId: Long,
        name: String,
        fullName: String,
        description: String?,
        language: String?,
        stars: Int,
        forks: Int
    ): Repo = repoRepository.findByGithubRepoId(githubRepoId)?.apply {
        if (isDeleted) activate()
        updateInfo(name, fullName, description, language)
        updateStats(stars, forks)
    } ?: repoRepository.save(Repo(githubRepoId, user, name, fullName, description, language, stars, forks))

    @Transactional
    fun updateRegister(repoId: UUID, userId: UUID, isRegistered: Boolean): Repo =
        getRepo(repoId).also {
            require(it.user.id == userId) { throw BusinessException(RepoError.REPO_ACCESS_DENIED) }
            it.updateRegister(isRegistered)
        }
}
