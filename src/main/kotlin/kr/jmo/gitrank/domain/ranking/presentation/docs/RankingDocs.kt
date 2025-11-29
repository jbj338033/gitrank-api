package kr.jmo.gitrank.domain.ranking.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jmo.gitrank.domain.ranking.presentation.response.RepoRankingResponse
import kr.jmo.gitrank.domain.ranking.presentation.response.UserRankingResponse
import kr.jmo.gitrank.global.pagination.PageResponse
import java.util.UUID

@Tag(name = "랭킹", description = "랭킹 API")
interface RankingDocs {
    @Operation(summary = "유저 랭킹 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getUserRankings(
        @Parameter(description = "정렬 기준: commits, stars, followers") sort: String,
        @Parameter(description = "기간: all, yearly, monthly, weekly") period: String,
        @Parameter(description = "페이지네이션 커서") cursor: UUID?,
        @Parameter(description = "조회 개수") limit: Int,
    ): PageResponse<UserRankingResponse>

    @Operation(summary = "레포지토리 랭킹 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getRepoRankings(
        @Parameter(description = "정렬 기준: stars, forks") sort: String,
        @Parameter(description = "페이지네이션 커서") cursor: UUID?,
        @Parameter(description = "조회 개수") limit: Int,
    ): PageResponse<RepoRankingResponse>
}
