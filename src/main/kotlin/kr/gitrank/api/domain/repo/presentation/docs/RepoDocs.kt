package kr.gitrank.api.domain.repo.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.gitrank.api.domain.repo.presentation.request.UpdateRegisterRequest
import kr.gitrank.api.domain.repo.presentation.response.RepoListResponse
import kr.gitrank.api.domain.repo.presentation.response.RepoResponse
import org.springframework.http.ResponseEntity
import java.util.UUID

@Tag(name = "Repositories", description = "Repository management APIs")
interface RepoDocs {

    @Operation(summary = "Get my repositories", description = "Get all repositories of the authenticated user")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved repositories"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getMyRepos(): ResponseEntity<RepoListResponse>

    @Operation(summary = "Update repository registration", description = "Register or unregister a repository for ranking")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully updated registration"),
            ApiResponse(responseCode = "401", description = "Unauthorized"),
            ApiResponse(responseCode = "403", description = "Access denied"),
            ApiResponse(responseCode = "404", description = "Repository not found")
        ]
    )
    fun updateRegister(
        @Parameter(description = "Repository ID") id: UUID,
        request: UpdateRegisterRequest
    ): ResponseEntity<RepoResponse>
}
