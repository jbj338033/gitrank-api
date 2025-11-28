package kr.gitrank.api.domain.repo.presentation.controller

import jakarta.validation.Valid
import kr.gitrank.api.domain.repo.application.RepoService
import kr.gitrank.api.domain.repo.presentation.docs.RepoDocs
import kr.gitrank.api.domain.repo.presentation.request.UpdateRegisterRequest
import kr.gitrank.api.domain.repo.presentation.response.RepoListResponse
import kr.gitrank.api.domain.repo.presentation.response.RepoResponse
import kr.gitrank.api.global.security.holder.SecurityHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/repos")
class RepoController(
    private val repoService: RepoService,
    private val securityHolder: SecurityHolder
) : RepoDocs {

    @GetMapping("/me")
    override fun getMyRepos(@RequestParam(required = false) query: String?): ResponseEntity<RepoListResponse> =
        ResponseEntity.ok(RepoListResponse(repoService.getReposByUserId(securityHolder.getCurrentUserId(), query).map(RepoResponse::from)))

    @PatchMapping("/{id}/register")
    override fun updateRegister(@PathVariable id: UUID, @Valid @RequestBody request: UpdateRegisterRequest): ResponseEntity<RepoResponse> =
        ResponseEntity.ok(RepoResponse.from(repoService.updateRegister(id, securityHolder.getCurrentUserId(), request.registered)))
}
