package kr.jmo.gitrank.domain.repo.domain.error

import kr.jmo.gitrank.global.error.BaseError
import org.springframework.http.HttpStatus

enum class RepoError(
    override val status: HttpStatus,
    override val message: String,
) : BaseError {
    REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "Repository not found"),
    REPO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "You don't have permission to access this repository"),
}
