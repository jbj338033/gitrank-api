package kr.jmo.gitrank.domain.repo.application.service

import kr.jmo.gitrank.domain.repo.domain.error.RepoError
import kr.jmo.gitrank.domain.repo.domain.repository.RepoRepository
import kr.jmo.gitrank.domain.repo.presentation.response.RepoResponse
import kr.jmo.gitrank.global.error.BusinessException
import kr.jmo.gitrank.global.security.holder.SecurityHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RepoService(
    private val repoRepository: RepoRepository,
    private val securityHolder: SecurityHolder,
) {
    fun getMyRepos(query: String?): List<RepoResponse> {
        val repos = repoRepository.searchByUserIdAndQuery(securityHolder.userId(), query)

        return repos.map(::RepoResponse)
    }

    @Transactional
    fun updateRegister(repoId: UUID, registered: Boolean) {
        val repo = repoRepository.findByIdAndDeletedAtIsNull(repoId)
            ?: throw BusinessException(RepoError.REPO_NOT_FOUND)

        if (!repo.isOwner(securityHolder.userId())) {
            throw BusinessException(RepoError.REPO_ACCESS_DENIED)
        }

        repo.updateRegister(registered)
    }
}
