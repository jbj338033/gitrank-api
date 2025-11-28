package kr.gitrank.api.domain.user.presentation.docs

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.gitrank.api.domain.user.presentation.request.UpdateVisibilityRequest
import kr.gitrank.api.domain.user.presentation.response.UserResponse
import org.springframework.http.ResponseEntity

@Tag(name = "Users", description = "User management APIs")
interface UserDocs {

    @Operation(summary = "Get my profile", description = "Get the authenticated user's profile")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully retrieved user profile"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun getMe(): ResponseEntity<UserResponse>

    @Operation(summary = "Update visibility", description = "Update the ranking visibility setting")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Successfully updated visibility"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun updateVisibility(request: UpdateVisibilityRequest): ResponseEntity<UserResponse>

    @Operation(summary = "Delete account", description = "Delete the authenticated user's account")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Successfully deleted account"),
            ApiResponse(responseCode = "401", description = "Unauthorized")
        ]
    )
    fun deleteMe(): ResponseEntity<Unit>
}
