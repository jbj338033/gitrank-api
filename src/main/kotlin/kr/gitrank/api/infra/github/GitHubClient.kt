package kr.gitrank.api.infra.github

import kr.gitrank.api.global.security.oauth2.github.GitHubOAuth2Properties
import kr.gitrank.api.infra.github.dto.GitHubAccessTokenResponse
import kr.gitrank.api.infra.github.dto.GitHubRepoDto
import kr.gitrank.api.infra.github.dto.GitHubUserDto
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class GitHubClient(
    private val webClient: WebClient,
    private val gitHubOAuth2Properties: GitHubOAuth2Properties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val GITHUB_API_BASE_URL = "https://api.github.com"
        private const val GITHUB_ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token"
    }

    fun getAccessToken(code: String): String? {
        return try {
            val response = webClient.post()
                .uri(GITHUB_ACCESS_TOKEN_URL)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(
                    mapOf(
                        "client_id" to gitHubOAuth2Properties.clientId,
                        "client_secret" to gitHubOAuth2Properties.clientSecret,
                        "code" to code
                    )
                )
                .retrieve()
                .bodyToMono(GitHubAccessTokenResponse::class.java)
                .block()

            response?.accessToken
        } catch (e: Exception) {
            log.error("Failed to get GitHub access token", e)
            null
        }
    }

    fun getUser(accessToken: String): GitHubUserDto? {
        return try {
            webClient.get()
                .uri("$GITHUB_API_BASE_URL/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(GitHubUserDto::class.java)
                .block()
        } catch (e: Exception) {
            log.error("Failed to get GitHub user", e)
            null
        }
    }

    fun getUserRepos(accessToken: String, username: String): List<GitHubRepoDto> {
        return try {
            webClient.get()
                .uri("$GITHUB_API_BASE_URL/users/$username/repos?per_page=100&sort=updated")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToFlux(GitHubRepoDto::class.java)
                .collectList()
                .block() ?: emptyList()
        } catch (e: Exception) {
            log.error("Failed to get GitHub repos for user: $username", e)
            emptyList()
        }
    }

    fun getUserContributionStats(accessToken: String, username: String): Int {
        // Using GitHub GraphQL API to get contribution count
        val query = """
            query {
                user(login: "$username") {
                    contributionsCollection {
                        totalCommitContributions
                    }
                }
            }
        """.trimIndent()

        return try {
            val response = webClient.post()
                .uri("$GITHUB_API_BASE_URL/graphql")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(mapOf("query" to query))
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()

            @Suppress("UNCHECKED_CAST")
            val data = response?.get("data") as? Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val user = data?.get("user") as? Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val contributions = user?.get("contributionsCollection") as? Map<String, Any>
            (contributions?.get("totalCommitContributions") as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            log.error("Failed to get GitHub contribution stats for user: $username", e)
            0
        }
    }
}
