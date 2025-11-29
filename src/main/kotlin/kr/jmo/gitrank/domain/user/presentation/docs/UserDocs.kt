package kr.jmo.gitrank.domain.user.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jmo.gitrank.domain.user.presentation.request.UpdateVisibilityRequest
import kr.jmo.gitrank.domain.user.presentation.response.UserResponse

@Tag(name = "유저", description = "유저 API")
interface UserDocs {
    @Operation(summary = "내 정보 조회")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
    )
    fun getMe(): UserResponse

    @Operation(summary = "공개 여부 수정")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "수정 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
    )
    fun updateVisibility(request: UpdateVisibilityRequest)

    @Operation(summary = "회원 탈퇴")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "탈퇴 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
    )
    fun deleteMe()

    @Operation(summary = "GitHub 동기화")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "동기화 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
    )
    fun sync()
}
