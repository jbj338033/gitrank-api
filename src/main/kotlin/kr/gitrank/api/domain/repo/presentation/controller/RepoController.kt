package kr.gitrank.api.domain.repo.presentation.controller

import jakarta.validation.Valid
import kr.gitrank.api.domain.repo.application.RepoService
import kr.gitrank.api.domain.repo.presentation.docs.RepoDocs
import kr.gitrank.api.domain.repo.presentation.request.UpdateRegisterRequest
import kr.gitrank.api.domain.repo.presentation.response.RepoListResponse
import kr.gitrank.api.domain.repo.presentation.response.RepoResponse
import kr.gitrank.api.global.security.holder.SecurityHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/repos")
class RepoController(
    private val repoService: RepoService,
    private val securityHolder: SecurityHolder
) : RepoDocs {

    @GetMapping("/me")
    override fun getMyRepos(): ResponseEntity<RepoListResponse> {
        val userId = securityHolder.getCurrentUserId()
        val repos = repoService.getReposByUserId(userId)
        return ResponseEntity.ok(
            RepoListResponse(repos = repos.map { RepoResponse.from(it) })
        )
    }

    @PatchMapping("/{id}/register")
    override fun updateRegister(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRegisterRequest
    ): ResponseEntity<RepoResponse> {
        val userId = securityHolder.getCurrentUserId()
        val repo = repoService.updateRegister(id, userId, request.isRegistered)
        return ResponseEntity.ok(RepoResponse.from(repo))
    }
}
