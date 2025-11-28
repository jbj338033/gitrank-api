package kr.gitrank.api.domain.auth.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.gitrank.api.domain.auth.presentation.request.RefreshRequest
import kr.gitrank.api.domain.auth.presentation.response.LoginEvent
import kr.gitrank.api.domain.auth.presentation.response.TokenResponse
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux

@Tag(name = "Auth", description = "Authentication APIs")
interface AuthDocs {

    @Operation(summary = "GitHub OAuth callback", description = "Handle GitHub OAuth with real-time progress updates via Server-Sent Events")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "SSE stream with progress events"),
            ApiResponse(responseCode = "401", description = "GitHub authentication failed")
        ]
    )
    fun githubCallback(@Parameter(description = "GitHub OAuth code") code: String): Flux<LoginEvent>

    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully refreshed tokens"),
            ApiResponse(responseCode = "401", description = "Invalid refresh token")
        ]
    )
    fun refresh(request: RefreshRequest): ResponseEntity<TokenResponse>

    @Operation(summary = "Logout", description = "Invalidate refresh token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Successfully logged out"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun logout(): ResponseEntity<Unit>
}
