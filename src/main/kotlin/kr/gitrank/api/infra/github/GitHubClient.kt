package kr.gitrank.api.infra.github

import kr.gitrank.api.global.security.oauth2.github.GitHubOAuth2Properties
import kr.gitrank.api.infra.github.dto.*
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.Year

@Component
class GitHubClient(
    private val webClient: WebClient,
    private val properties: GitHubOAuth2Properties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun fetchAccessToken(code: String): String? = runCatching {
        webClient.post()
            .uri(TOKEN_URL)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(mapOf(
                "client_id" to properties.clientId,
                "client_secret" to properties.clientSecret,
                "code" to code
            ))
            .retrieve()
            .bodyToMono(GitHubAccessToken::class.java)
            .block()?.accessToken
    }.onFailure { log.error("Failed to fetch access token", it) }.getOrNull()

    fun fetchUser(token: String): GitHubUser? = runCatching {
        webClient.get()
            .uri("$API_URL/user")
            .headers { it.setBearerAuth(token) }
            .retrieve()
            .bodyToMono(GitHubUser::class.java)
            .block()
    }.onFailure { log.error("Failed to fetch user", it) }.getOrNull()

    fun fetchRepos(token: String, username: String): List<GitHubRepo> = runCatching {
        webClient.get()
            .uri("$API_URL/users/$username/repos?per_page=100&sort=updated")
            .headers { it.setBearerAuth(token) }
            .retrieve()
            .bodyToFlux(GitHubRepo::class.java)
            .collectList()
            .block()
    }.onFailure { log.error("Failed to fetch repos: $username", it) }.getOrNull() ?: emptyList()

    fun fetchTotalContributions(token: String): Int =
        fetchContributionYears(token).sumOf { fetchYearlyContributions(token, it) }

    private fun fetchContributionYears(token: String): List<Int> = runCatching {
        val query = "query { viewer { contributionsCollection { contributionYears } } }"
        graphql<ViewerResponse>(token, query)
            ?.viewer
            ?.contributionsCollection
            ?.contributionYears
            ?: emptyList()
    }.onFailure { log.error("Failed to fetch contribution years", it) }.getOrDefault(emptyList())

    private fun fetchYearlyContributions(token: String, year: Int): Int = runCatching {
        val endDate = if (year == Year.now().value) LocalDate.now() else LocalDate.of(year, 12, 31)
        val query = """
            query {
                viewer {
                    contributionsCollection(from: "$year-01-01T00:00:00Z", to: "${endDate}T23:59:59Z") {
                        contributionCalendar { totalContributions }
                    }
                }
            }
        """.trimIndent()

        graphql<ViewerResponse>(token, query)
            ?.viewer
            ?.contributionsCollection
            ?.contributionCalendar
            ?.totalContributions
            ?: 0
    }.onFailure { log.error("Failed to fetch contributions for year: $year", it) }.getOrDefault(0)

    private inline fun <reified T> graphql(token: String, query: String): T? =
        webClient.post()
            .uri("$API_URL/graphql")
            .headers { it.setBearerAuth(token) }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(mapOf("query" to query))
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<GraphQLResponse<T>>() {})
            .block()
            ?.data

    companion object {
        private const val API_URL = "https://api.github.com"
        private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    }
}
