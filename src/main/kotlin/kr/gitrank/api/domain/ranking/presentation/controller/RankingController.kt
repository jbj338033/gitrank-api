package kr.gitrank.api.domain.ranking.presentation.controller

import kr.gitrank.api.domain.ranking.application.RankingService
import kr.gitrank.api.domain.ranking.presentation.docs.RankingDocs
import kr.gitrank.api.domain.ranking.presentation.response.RepoRankingResponse
import kr.gitrank.api.domain.ranking.presentation.response.UserRankingResponse
import kr.gitrank.api.global.response.CursorRequest
import kr.gitrank.api.global.response.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rankings")
class RankingController(
    private val rankingService: RankingService
) : RankingDocs {

    @GetMapping("/users")
    override fun getUserRankings(
        @RequestParam(defaultValue = "commits") sort: String?,
        @RequestParam(defaultValue = "all") period: String?,
        @RequestParam language: String?,
        @RequestParam cursor: UUID?,
        @RequestParam limit: Int?
    ): ResponseEntity<PageResponse<UserRankingResponse>> = ResponseEntity.ok(
        rankingService.getUserRankings(sort ?: "commits", period ?: "all", language, CursorRequest(cursor, limit ?: CursorRequest.DEFAULT_LIMIT))
    )

    @GetMapping("/repos")
    override fun getRepoRankings(
        @RequestParam(defaultValue = "stars") sort: String?,
        @RequestParam language: String?,
        @RequestParam cursor: UUID?,
        @RequestParam limit: Int?
    ): ResponseEntity<PageResponse<RepoRankingResponse>> = ResponseEntity.ok(
        rankingService.getRepoRankings(sort ?: "stars", language, CursorRequest(cursor, limit ?: CursorRequest.DEFAULT_LIMIT))
    )
}
