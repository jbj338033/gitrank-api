package kr.gitrank.api.domain.auth.presentation.controller

import jakarta.validation.Valid
import kr.gitrank.api.domain.auth.application.AuthService
import kr.gitrank.api.domain.auth.presentation.docs.AuthDocs
import kr.gitrank.api.domain.auth.presentation.request.RefreshRequest
import kr.gitrank.api.domain.auth.presentation.response.LoginEvent
import kr.gitrank.api.domain.auth.presentation.response.TokenResponse
import kr.gitrank.api.global.security.holder.SecurityHolder
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val securityHolder: SecurityHolder
) : AuthDocs {

    @GetMapping("/github/callback", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    override fun githubCallback(@RequestParam code: String): Flux<LoginEvent> =
        authService.loginWithGitHub(code)

    @PostMapping("/refresh")
    override fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> =
        ResponseEntity.ok(authService.refresh(request.refreshToken))

    @PostMapping("/logout")
    override fun logout(): ResponseEntity<Unit> {
        authService.logout(securityHolder.getCurrentUserId())
        return ResponseEntity.noContent().build()
    }
}
