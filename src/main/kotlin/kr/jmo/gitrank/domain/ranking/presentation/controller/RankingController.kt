package kr.jmo.gitrank.domain.ranking.presentation.controller

import kr.jmo.gitrank.domain.ranking.application.service.RankingService
import kr.jmo.gitrank.domain.ranking.presentation.docs.RankingDocs
import kr.jmo.gitrank.global.pagination.Cursor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rankings")
class RankingController(
    private val rankingService: RankingService,
) : RankingDocs {
    @GetMapping("/users")
    override fun getUserRankings(
        @RequestParam(defaultValue = "commits") sort: String,
        @RequestParam(defaultValue = "all") period: String,
        @RequestParam cursor: UUID?,
        @RequestParam(defaultValue = "30") limit: Int,
    ) = rankingService.getUserRankings(sort, period, Cursor(cursor, limit))

    @GetMapping("/repos")
    override fun getRepoRankings(
        @RequestParam(defaultValue = "stars") sort: String,
        @RequestParam cursor: UUID?,
        @RequestParam(defaultValue = "30") limit: Int,
    ) = rankingService.getRepoRankings(sort, Cursor(cursor, limit))
}
