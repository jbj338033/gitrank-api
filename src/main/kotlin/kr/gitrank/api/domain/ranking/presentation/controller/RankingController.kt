package kr.gitrank.api.domain.ranking.presentation.controller

import kr.gitrank.api.domain.ranking.application.RankingService
import kr.gitrank.api.domain.ranking.presentation.docs.RankingDocs
import kr.gitrank.api.domain.ranking.presentation.response.RepoRankingResponse
import kr.gitrank.api.domain.ranking.presentation.response.UserRankingResponse
import kr.gitrank.api.global.response.CursorRequest
import kr.gitrank.api.global.response.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
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
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) cursor: UUID?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<PageResponse<UserRankingResponse>> {
        val cursorRequest = CursorRequest(
            cursor = cursor,
            limit = limit ?: CursorRequest.DEFAULT_LIMIT
        )

        val rankings = rankingService.getUserRankings(
            sort = sort ?: "commits",
            period = period ?: "all",
            language = language,
            cursorRequest = cursorRequest
        )

        return ResponseEntity.ok(rankings)
    }

    @GetMapping("/repos")
    override fun getRepoRankings(
        @RequestParam(defaultValue = "stars") sort: String?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) cursor: UUID?,
        @RequestParam(required = false) limit: Int?
    ): ResponseEntity<PageResponse<RepoRankingResponse>> {
        val cursorRequest = CursorRequest(
            cursor = cursor,
            limit = limit ?: CursorRequest.DEFAULT_LIMIT
        )

        val rankings = rankingService.getRepoRankings(
            sort = sort ?: "stars",
            language = language,
            cursorRequest = cursorRequest
        )

        return ResponseEntity.ok(rankings)
    }
}
