package kr.gitrank.api.domain.user.presentation.controller

import jakarta.validation.Valid
import kr.gitrank.api.domain.user.application.UserService
import kr.gitrank.api.domain.user.presentation.docs.UserDocs
import kr.gitrank.api.domain.user.presentation.request.UpdateVisibilityRequest
import kr.gitrank.api.domain.user.presentation.response.UserResponse
import kr.gitrank.api.global.security.holder.SecurityHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val securityHolder: SecurityHolder
) : UserDocs {

    @GetMapping("/me")
    override fun getMe(): ResponseEntity<UserResponse> {
        val userId = securityHolder.getCurrentUserId()
        val user = userService.getUserById(userId)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @PatchMapping("/me/visibility")
    override fun updateVisibility(
        @Valid @RequestBody request: UpdateVisibilityRequest
    ): ResponseEntity<UserResponse> {
        val userId = securityHolder.getCurrentUserId()
        val user = userService.updateVisibility(userId, request.isVisible)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @DeleteMapping("/me")
    override fun deleteMe(): ResponseEntity<Unit> {
        val userId = securityHolder.getCurrentUserId()
        userService.deleteUser(userId)
        return ResponseEntity.noContent().build()
    }
}
