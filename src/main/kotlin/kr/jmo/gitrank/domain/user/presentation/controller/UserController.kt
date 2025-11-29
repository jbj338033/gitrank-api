package kr.jmo.gitrank.domain.user.presentation.controller

import jakarta.validation.Valid
import kr.jmo.gitrank.domain.user.application.service.UserService
import kr.jmo.gitrank.domain.user.presentation.docs.UserDocs
import kr.jmo.gitrank.domain.user.presentation.request.UpdateVisibilityRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) : UserDocs {
    @GetMapping("/me")
    override fun getMe() = userService.getMe()

    @PatchMapping("/me/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun updateVisibility(
        @Valid @RequestBody request: UpdateVisibilityRequest,
    ) = userService.updateVisibility(request.visible)

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteMe() = userService.deleteMe()

    @PostMapping("/me/sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun sync() = userService.sync()
}
