package kr.jmo.gitrank.domain.auth.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jmo.gitrank.domain.auth.presentation.request.LogoutRequest
import kr.jmo.gitrank.domain.auth.presentation.request.RefreshRequest
import kr.jmo.gitrank.domain.auth.presentation.response.LoginEvent
import kr.jmo.gitrank.domain.auth.presentation.response.TokenResponse
import reactor.core.publisher.Flux

@Tag(name = "인증", description = "인증 API")
interface AuthDocs {
    @Operation(summary = "GitHub OAuth 콜백")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "SSE 스트림으로 진행 상태 전송"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
    )
    fun githubCallback(
        @Parameter(description = "GitHub OAuth 코드") code: String,
    ): Flux<LoginEvent>

    @Operation(summary = "토큰 갱신")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "갱신 성공"),
        ApiResponse(responseCode = "401", description = "유효하지 않은 토큰"),
    )
    fun refresh(request: RefreshRequest): TokenResponse

    @Operation(summary = "로그아웃")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "로그아웃 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
    )
    fun logout(request: LogoutRequest)
}
