package kr.jmo.gitrank.domain.auth.application.service

import kr.jmo.gitrank.domain.auth.domain.entity.RefreshToken
import kr.jmo.gitrank.domain.auth.domain.error.AuthError
import kr.jmo.gitrank.domain.auth.domain.repository.RefreshTokenRepository
import kr.jmo.gitrank.domain.auth.presentation.response.LoginEvent
import kr.jmo.gitrank.domain.auth.presentation.response.LoginStep
import kr.jmo.gitrank.domain.auth.presentation.response.TokenResponse
import kr.jmo.gitrank.domain.user.domain.entity.User
import kr.jmo.gitrank.domain.user.domain.error.UserError
import kr.jmo.gitrank.domain.user.domain.repository.UserRepository
import kr.jmo.gitrank.domain.user.presentation.response.UserResponse
import kr.jmo.gitrank.global.error.BusinessException
import kr.jmo.gitrank.global.security.jwt.enums.JwtType
import kr.jmo.gitrank.global.security.jwt.parser.JwtParser
import kr.jmo.gitrank.global.security.jwt.properties.JwtProperties
import kr.jmo.gitrank.global.security.jwt.provider.JwtProvider
import kr.jmo.gitrank.global.security.jwt.validator.JwtValidator
import kr.jmo.gitrank.infra.github.client.GitHubClient
import kr.jmo.gitrank.infra.github.service.GitHubSyncService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.LocalDateTime

@Service
class AuthService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val gitHubClient: GitHubClient,
    private val gitHubSyncService: GitHubSyncService,
    private val jwtProvider: JwtProvider,
    private val jwtValidator: JwtValidator,
    private val jwtParser: JwtParser,
    private val jwtProperties: JwtProperties,
) {
    fun githubCallback(code: String): Flux<LoginEvent> {
        val sink = Sinks.many().unicast().onBackpressureBuffer<LoginEvent>()

        Thread.startVirtualThread {
            try {
                sink.tryEmitNext(LoginEvent.Progress(LoginStep.AUTHENTICATING))

                val githubToken =
                    gitHubClient.fetchAccessToken(code)
                        ?: throw BusinessException(AuthError.INVALID_AUTHORIZATION_CODE)
                val githubUser =
                    gitHubClient.fetchUser(githubToken.accessToken)
                        ?: throw BusinessException(AuthError.GITHUB_AUTH_FAILED)

                val user =
                    userRepository.findByGithubId(githubUser.id)?.apply {
                        if (isDeleted) activate()
                        updateProfile(githubUser.login, githubUser.avatarUrl)
                    } ?: User(githubUser.id, githubUser.login, githubUser.avatarUrl)

                user.updateGitHubTokens(
                    githubToken.accessToken,
                    githubToken.refreshToken,
                    githubToken.refreshTokenExpiresIn,
                )
                val saved = userRepository.save(user)

                sink.tryEmitNext(LoginEvent.Progress(LoginStep.SYNCING))
                gitHubSyncService.syncRepos(saved.id, githubToken.accessToken)
                gitHubSyncService.syncStats(saved.id, githubToken.accessToken)

                val accessToken = jwtProvider.createAccessToken(saved.id)
                val refreshToken = jwtProvider.createRefreshToken(saved.id)
                refreshTokenRepository.save(
                    RefreshToken(saved.id, refreshToken, LocalDateTime.now().plusSeconds(jwtProperties.refreshExpiry)),
                )

                sink.tryEmitNext(LoginEvent.Complete(TokenResponse(accessToken, refreshToken, UserResponse(saved))))
            } catch (e: Exception) {
                sink.tryEmitNext(LoginEvent.Error(e.message ?: "Unknown error"))
            } finally {
                sink.tryEmitComplete()
            }
        }

        return sink.asFlux()
    }

    @Transactional
    fun refresh(token: String): TokenResponse {
        jwtValidator.validate(token, JwtType.REFRESH)

        val stored =
            refreshTokenRepository.findByTokenAndDeletedAtIsNull(token)
                ?: throw BusinessException(AuthError.INVALID_REFRESH_TOKEN)

        if (stored.isExpired()) {
            throw BusinessException(AuthError.EXPIRED_TOKEN)
        }

        val user =
            userRepository.findByIdAndDeletedAtIsNull(jwtParser.getUserId(token))
                ?: throw BusinessException(UserError.USER_NOT_FOUND)

        stored.delete()

        val accessToken = jwtProvider.createAccessToken(user.id)
        val refreshToken = jwtProvider.createRefreshToken(user.id)
        refreshTokenRepository.save(
            RefreshToken(user.id, refreshToken, LocalDateTime.now().plusSeconds(jwtProperties.refreshExpiry)),
        )

        return TokenResponse(accessToken, refreshToken)
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.findByTokenAndDeletedAtIsNull(refreshToken)?.delete()
    }
}
