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
@Transactional(readOnly = true)
class RepoService(
    private val repoRepository: RepoRepository
) {

    fun getRepoById(id: UUID): Repo {
        return repoRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw BusinessException(RepoError.REPO_NOT_FOUND)
    }

    fun getReposByUserId(userId: UUID): List<Repo> {
        return repoRepository.findByUserIdOrderByStarsDesc(userId)
    }

    fun getRepoByGithubRepoId(githubRepoId: Long): Repo? {
        return repoRepository.findByGithubRepoId(githubRepoId)
    }

    @Transactional
    fun createRepo(
        user: User,
        githubRepoId: Long,
        name: String,
        fullName: String,
        description: String?,
        language: String?,
        stars: Int,
        forks: Int
    ): Repo {
        val repo = Repo(
            githubRepoId = githubRepoId,
            user = user,
            name = name,
            fullName = fullName,
            description = description,
            language = language,
            stars = stars,
            forks = forks
        )
        return repoRepository.save(repo)
    }

    @Transactional
    fun getOrCreateRepo(
        user: User,
        githubRepoId: Long,
        name: String,
        fullName: String,
        description: String?,
        language: String?,
        stars: Int,
        forks: Int
    ): Repo {
        return repoRepository.findByGithubRepoId(githubRepoId)?.also { repo ->
            if (repo.isDeleted) {
                repo.activate()
            }
            repo.updateInfo(name, fullName, description, language)
            repo.updateStats(stars, forks)
        } ?: createRepo(user, githubRepoId, name, fullName, description, language, stars, forks)
    }

    @Transactional
    fun updateRegister(repoId: UUID, userId: UUID, isRegistered: Boolean): Repo {
        val repo = getRepoById(repoId)

        if (repo.user.id != userId) {
            throw BusinessException(RepoError.REPO_ACCESS_DENIED)
        }

        repo.updateRegister(isRegistered)
        return repo
    }

    fun getRegisteredRepos(language: String?): List<Repo> {
        return repoRepository.findRegisteredReposByLanguage(language)
    }
}
