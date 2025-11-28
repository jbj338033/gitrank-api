package kr.gitrank.api.domain.auth.presentation.controller

import jakarta.validation.Valid
import kr.gitrank.api.domain.auth.application.AuthService
import kr.gitrank.api.domain.auth.presentation.docs.AuthDocs
import kr.gitrank.api.domain.auth.presentation.request.GitHubCallbackRequest
import kr.gitrank.api.domain.auth.presentation.request.RefreshRequest
import kr.gitrank.api.domain.auth.presentation.response.TokenResponse
import kr.gitrank.api.global.security.holder.SecurityHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val securityHolder: SecurityHolder
) : AuthDocs {

    @PostMapping("/github/callback")
    override fun githubCallback(@Valid @RequestBody request: GitHubCallbackRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(authService.loginWithGitHub(request.code))

    @PostMapping("/refresh")
    override fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(authService.refresh(request.refreshToken))

    @PostMapping("/logout")
    override fun logout(): ResponseEntity<Unit> {
        authService.logout(securityHolder.getCurrentUserId())
        return ResponseEntity.noContent().build()
    }
}
