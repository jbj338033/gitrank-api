package kr.gitrank.api.domain.auth.application

import kr.gitrank.api.domain.auth.domain.entity.RefreshToken
import kr.gitrank.api.domain.auth.domain.error.AuthError
import kr.gitrank.api.domain.auth.domain.repository.RefreshTokenRepository
 import kr.gitrank.api.domain.auth.presentation.response.LoginEvent
import kr.gitrank.api.domain.auth.presentation.response.TokenResponse
import kr.gitrank.api.domain.user.application.UserService
import kr.gitrank.api.domain.user.presentation.response.UserResponse
import kr.gitrank.api.global.error.BusinessException
import kr.gitrank.api.global.security.jwt.enums.JwtType
import kr.gitrank.api.global.security.jwt.properties.JwtProperties
import kr.gitrank.api.global.security.jwt.provider.JwtProvider
import kr.gitrank.api.global.security.jwt.validator.JwtValidator
import kr.gitrank.api.infra.github.GitHubClient
import kr.gitrank.api.infra.github.GitHubSyncService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userService: UserService,
    private val gitHubClient: GitHubClient,
    private val gitHubSyncService: GitHubSyncService,
    private val jwtProvider: JwtProvider,
    private val jwtValidator: JwtValidator,
    private val jwtProperties: JwtProperties
) {

    fun loginWithGitHub(code: String): Flux<LoginEvent> {
        val sink = Sinks.many().unicast().onBackpressureBuffer<LoginEvent>()

        Thread.startVirtualThread {
            runCatching {
                sink.tryEmitNext(LoginEvent.progress("authenticating"))

                val token = gitHubClient.fetchAccessToken(code)
                    ?: throw BusinessException(AuthError.INVALID_AUTHORIZATION_CODE)
                val githubUser = gitHubClient.fetchUser(token)
                    ?: throw BusinessException(AuthError.GITHUB_AUTH_FAILED)

                sink.tryEmitNext(LoginEvent.progress("creating_user"))
                val user = userService.upsertUser(githubUser.id, githubUser.login, githubUser.avatarUrl)

                sink.tryEmitNext(LoginEvent.progress("syncing_repos"))
                gitHubSyncService.syncRepos(user, token)

                sink.tryEmitNext(LoginEvent.progress("syncing_stats"))
                gitHubSyncService.syncStats(user, token)

                sink.tryEmitNext(LoginEvent.progress("issuing_tokens"))
                val refreshedUser = userService.getUser(user.id)
                val tokenResponse = issueTokens(refreshedUser.id, refreshedUser.username)
                    .copy(user = UserResponse.from(refreshedUser))

                sink.tryEmitNext(LoginEvent.complete(tokenResponse))
            }.onFailure {
                sink.tryEmitNext(LoginEvent.error(it.message ?: "Unknown error"))
            }
            sink.tryEmitComplete()
        }

        return sink.asFlux()
    }

    @Transactional
    fun refresh(token: String): TokenResponse {
        require(jwtValidator.validateToken(token, JwtType.REFRESH)) { throw BusinessException(AuthError.INVALID_REFRESH_TOKEN) }

        val stored = refreshTokenRepository.findByToken(token) ?: throw BusinessException(AuthError.INVALID_REFRESH_TOKEN)
        require(!stored.isExpired() && !stored.isDeleted) { throw BusinessException(AuthError.EXPIRED_TOKEN) }

        stored.delete()
        val user = userService.getUser(jwtValidator.getUserId(token))
        return issueTokens(user.id, user.username)
    }

    @Transactional
    fun logout(userId: UUID) = refreshTokenRepository.invalidateAllByUserId(userId)

    private fun issueTokens(userId: UUID, username: String): TokenResponse {
        val accessToken = jwtProvider.createAccessToken(userId, username)
        val refreshToken = jwtProvider.createRefreshToken(userId, username)

        val user = userService.getUser(userId)
        val expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshExpiry)
        refreshTokenRepository.save(RefreshToken(user, refreshToken, expiresAt))

        return TokenResponse(accessToken, refreshToken)
    }
}
