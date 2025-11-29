package kr.jmo.gitrank.domain.repo.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jmo.gitrank.domain.repo.presentation.request.UpdateRegisterRequest
import kr.jmo.gitrank.domain.repo.presentation.response.RepoResponse
import java.util.UUID

@Tag(name = "레포지토리", description = "레포지토리 API")
interface RepoDocs {
    @Operation(summary = "내 레포지토리 조회")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "조회 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
    )
    fun getMyRepos(@Parameter(description = "검색어") query: String?): List<RepoResponse>

    @Operation(summary = "랭킹 등록 여부 수정")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "수정 성공"),
        ApiResponse(responseCode = "401", description = "인증 필요"),
        ApiResponse(responseCode = "403", description = "접근 권한 없음"),
        ApiResponse(responseCode = "404", description = "레포지토리 없음"),
    )
    fun updateRegister(
        @Parameter(description = "레포지토리 ID") id: UUID,
        request: UpdateRegisterRequest,
    )
}
