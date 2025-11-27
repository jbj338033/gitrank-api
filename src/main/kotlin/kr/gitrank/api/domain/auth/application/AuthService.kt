package kr.gitrank.api.domain.auth.application

import kr.gitrank.api.domain.auth.domain.entity.RefreshToken
import kr.gitrank.api.domain.auth.domain.error.AuthError
import kr.gitrank.api.domain.auth.domain.repository.RefreshTokenRepository
import kr.gitrank.api.domain.auth.presentation.response.TokenResponse
import kr.gitrank.api.domain.user.application.UserService
import kr.gitrank.api.domain.user.presentation.response.UserResponse
import kr.gitrank.api.global.error.BusinessException
import kr.gitrank.api.global.security.jwt.enums.JwtType
import kr.gitrank.api.global.security.jwt.properties.JwtProperties
import kr.gitrank.api.global.security.jwt.provider.JwtProvider
import kr.gitrank.api.global.security.jwt.validator.JwtValidator
import kr.gitrank.api.infra.github.GitHubClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userService: UserService,
    private val gitHubClient: GitHubClient,
    private val jwtProvider: JwtProvider,
    private val jwtValidator: JwtValidator,
    private val jwtProperties: JwtProperties
) {

    @Transactional
    fun authenticateWithGitHub(code: String): TokenResponse {
        val accessToken = gitHubClient.getAccessToken(code)
            ?: throw BusinessException(AuthError.INVALID_AUTHORIZATION_CODE)

        val gitHubUser = gitHubClient.getUser(accessToken)
            ?: throw BusinessException(AuthError.GITHUB_AUTH_FAILED)

        val user = userService.getOrCreateUser(
            githubId = gitHubUser.id,
            username = gitHubUser.login,
            avatarUrl = gitHubUser.avatarUrl
        )

        val jwtAccessToken = jwtProvider.createAccessToken(user.id, user.username)
        val jwtRefreshToken = jwtProvider.createRefreshToken(user.id, user.username)

        saveRefreshToken(user.id, jwtRefreshToken)

        return TokenResponse(
            accessToken = jwtAccessToken,
            refreshToken = jwtRefreshToken,
            user = UserResponse.from(user)
        )
    }

    @Transactional
    fun refresh(refreshToken: String): TokenResponse {
        if (!jwtValidator.validateToken(refreshToken, JwtType.REFRESH)) {
            throw BusinessException(AuthError.INVALID_REFRESH_TOKEN)
        }

        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw BusinessException(AuthError.INVALID_REFRESH_TOKEN)

        if (storedToken.isExpired() || storedToken.isDeleted) {
            throw BusinessException(AuthError.EXPIRED_TOKEN)
        }

        val userId = jwtValidator.getUserId(refreshToken)
        val user = userService.getUserById(userId)

        // Invalidate old refresh token
        storedToken.delete()

        // Create new tokens
        val newAccessToken = jwtProvider.createAccessToken(user.id, user.username)
        val newRefreshToken = jwtProvider.createRefreshToken(user.id, user.username)

        saveRefreshToken(user.id, newRefreshToken)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    @Transactional
    fun logout(userId: UUID) {
        refreshTokenRepository.invalidateAllByUserId(userId)
    }

    private fun saveRefreshToken(userId: UUID, token: String) {
        val user = userService.getUserById(userId)
        val expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshExpiry)

        val refreshToken = RefreshToken(
            user = user,
            token = token,
            expiresAt = expiresAt
        )

        refreshTokenRepository.save(refreshToken)
    }
}
