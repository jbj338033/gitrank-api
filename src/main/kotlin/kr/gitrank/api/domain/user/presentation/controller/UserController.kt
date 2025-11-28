package kr.gitrank.api.domain.user.presentation.controller

import jakarta.validation.Valid
import kr.gitrank.api.domain.user.application.UserService
import kr.gitrank.api.domain.user.presentation.docs.UserDocs
import kr.gitrank.api.domain.user.presentation.request.UpdateVisibilityRequest
import kr.gitrank.api.domain.user.presentation.response.UserResponse
import kr.gitrank.api.global.security.holder.SecurityHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val securityHolder: SecurityHolder
) : UserDocs {

    @GetMapping("/me")
    override fun getMe(): ResponseEntity<UserResponse> =
        ResponseEntity.ok(UserResponse.from(userService.getUser(securityHolder.getCurrentUserId())))

    @PatchMapping("/me/visibility")
    override fun updateVisibility(@Valid @RequestBody request: UpdateVisibilityRequest): ResponseEntity<UserResponse> =
        ResponseEntity.ok(UserResponse.from(userService.updateVisibility(securityHolder.getCurrentUserId(), request.isVisible)))

    @DeleteMapping("/me")
    override fun deleteMe(): ResponseEntity<Unit> {
        userService.delete(securityHolder.getCurrentUserId())
        return ResponseEntity.noContent().build()
    }
}
