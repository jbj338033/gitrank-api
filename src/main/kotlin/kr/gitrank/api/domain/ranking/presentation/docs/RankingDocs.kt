package kr.gitrank.api.domain.ranking.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.gitrank.api.domain.ranking.presentation.response.RepoRankingResponse
import kr.gitrank.api.domain.ranking.presentation.response.UserRankingResponse
import kr.gitrank.api.global.response.PageResponse
import org.springframework.http.ResponseEntity
import java.util.UUID

@Tag(name = "Rankings", description = "Ranking APIs")
interface RankingDocs {

    @Operation(summary = "Get user rankings", description = "Get user rankings by various criteria")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved user rankings")
        ]
    )
    fun getUserRankings(
        @Parameter(description = "Sort by: commits, stars, followers") sort: String?,
        @Parameter(description = "Period: all, yearly, monthly, weekly") period: String?,
        @Parameter(description = "Programming language filter") language: String?,
        @Parameter(description = "Cursor for pagination") cursor: UUID?,
        @Parameter(description = "Number of items to return") limit: Int?
    ): ResponseEntity<PageResponse<UserRankingResponse>>

    @Operation(summary = "Get repository rankings", description = "Get repository rankings by various criteria")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved repository rankings")
        ]
    )
    fun getRepoRankings(
        @Parameter(description = "Sort by: stars, forks") sort: String?,
        @Parameter(description = "Programming language filter") language: String?,
        @Parameter(description = "Cursor for pagination") cursor: UUID?,
        @Parameter(description = "Number of items to return") limit: Int?
    ): ResponseEntity<PageResponse<RepoRankingResponse>>
}
