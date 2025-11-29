package kr.jmo.gitrank.domain.auth.presentation.controller

import jakarta.validation.Valid
import kr.jmo.gitrank.domain.auth.application.service.AuthService
import kr.jmo.gitrank.domain.auth.presentation.docs.AuthDocs
import kr.jmo.gitrank.domain.auth.presentation.request.LogoutRequest
import kr.jmo.gitrank.domain.auth.presentation.request.RefreshRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) : AuthDocs {
    @GetMapping("/github/callback", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    override fun githubCallback(
        @RequestParam code: String,
    ) = authService.githubCallback(code)

    @PostMapping("/refresh")
    override fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ) = authService.refresh(request.refreshToken)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun logout(
        @Valid @RequestBody request: LogoutRequest,
    ) = authService.logout(request.refreshToken)
}
