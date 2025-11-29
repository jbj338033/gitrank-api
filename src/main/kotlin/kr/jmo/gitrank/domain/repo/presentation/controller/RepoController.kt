package kr.jmo.gitrank.domain.repo.presentation.controller

import jakarta.validation.Valid
import kr.jmo.gitrank.domain.repo.application.service.RepoService
import kr.jmo.gitrank.domain.repo.presentation.docs.RepoDocs
import kr.jmo.gitrank.domain.repo.presentation.request.UpdateRegisterRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/repos")
class RepoController(
    private val repoService: RepoService,
) : RepoDocs {
    @GetMapping("/me")
    override fun getMyRepos(
        @RequestParam(required = false) query: String?,
    ) = repoService.getMyRepos(query)

    @PatchMapping("/{id}/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun updateRegister(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateRegisterRequest,
    ) = repoService.updateRegister(id, request.registered)
}
