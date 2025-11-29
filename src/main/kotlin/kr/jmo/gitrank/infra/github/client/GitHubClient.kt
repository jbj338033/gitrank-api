package kr.jmo.gitrank.infra.github.client

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.jmo.gitrank.infra.github.properties.GitHubProperties
import kr.jmo.gitrank.infra.github.data.GitHubAccessToken
import kr.jmo.gitrank.infra.github.data.GitHubRepo
import kr.jmo.gitrank.infra.github.data.GitHubUser
import kr.jmo.gitrank.infra.github.data.GraphQLResponse
import kr.jmo.gitrank.infra.github.data.ViewerResponse
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
    private val properties: GitHubProperties,
) {
    private val logger = KotlinLogging.logger {}

    fun fetchAccessToken(code: String) = runCatching {
        webClient.post()
            .uri(TOKEN_URL)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(mapOf(
                "client_id" to properties.clientId,
                "client_secret" to properties.clientSecret,
                "code" to code,
            ))
            .retrieve()
            .bodyToMono(GitHubAccessToken::class.java)
            .block()
    }.onFailure {
        logger.error(it) { "Failed to fetch access token" }
    }.getOrNull()

    fun refreshAccessToken(refreshToken: String) = runCatching {
        webClient.post()
            .uri(TOKEN_URL)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(mapOf(
                "client_id" to properties.clientId,
                "client_secret" to properties.clientSecret,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
            ))
            .retrieve()
            .bodyToMono(GitHubAccessToken::class.java)
            .block()
    }.onFailure {
        logger.error(it) { "Failed to refresh access token" }
    }.getOrNull()

    fun fetchUser(token: String) = runCatching {
        webClient.get()
            .uri("$API_URL/user")
            .headers { it.setBearerAuth(token) }
            .retrieve()
            .bodyToMono(GitHubUser::class.java)
            .block()
    }.onFailure {
        logger.error(it) { "Failed to fetch user" }
    }.getOrNull()

    fun fetchRepos(token: String): List<GitHubRepo> {
        return runCatching {
            val allRepos = mutableListOf<GitHubRepo>()
            var page = 1

            while (true) {
                val repos = webClient.get()
                    .uri("$API_URL/user/repos?per_page=100&page=$page&affiliation=owner")
                    .headers { it.setBearerAuth(token) }
                    .retrieve()
                    .bodyToFlux(GitHubRepo::class.java)
                    .collectList()
                    .block() ?: break

                if (repos.isEmpty()) break
                allRepos.addAll(repos)
                if (repos.size < 100) break
                page++
            }

            allRepos
        }.onFailure {
            logger.error(it) { "Failed to fetch repos" }
        }.getOrNull() ?: emptyList()
    }

    fun fetchContributions(token: String): ContributionStats {
        val years = fetchContributionYears(token)
        val total = years.sumOf { fetchContributionsBetween(token, LocalDate.of(it, 1, 1), yearEndDate(it)) }

        val now = LocalDate.now()
        val yearly = fetchContributionsBetween(token, now.withDayOfYear(1), now)
        val monthly = fetchContributionsBetween(token, now.withDayOfMonth(1), now)
        val weekly = fetchContributionsBetween(token, now.minusDays(now.dayOfWeek.value.toLong() - 1), now)

        return ContributionStats(total, yearly, monthly, weekly)
    }

    private fun fetchContributionYears(token: String) = runCatching {
        val query = "query { viewer { contributionsCollection { contributionYears } } }"

        graphql<ViewerResponse>(token, query)
            ?.viewer
            ?.contributionsCollection
            ?.contributionYears ?: emptyList()
    }.onFailure {
        logger.error(it) { "Failed to fetch contribution years" }
    }.getOrDefault(emptyList())

    private fun fetchContributionsBetween(token: String, from: LocalDate, to: LocalDate) = runCatching {
        val query = """
            query {
                viewer {
                    contributionsCollection(from: "${from}T00:00:00Z", to: "${to}T23:59:59Z") {
                        contributionCalendar { totalContributions }
                    }
                }
            }
        """.trimIndent()

        graphql<ViewerResponse>(token, query)
            ?.viewer
            ?.contributionsCollection
            ?.contributionCalendar
            ?.totalContributions ?: 0
    }.onFailure {
        logger.error(it) { "Failed to fetch contributions: $from ~ $to" }
    }.getOrDefault(0)

    private fun yearEndDate(year: Int) =
        if (year == Year.now().value) LocalDate.now() else LocalDate.of(year, 12, 31)

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

    data class ContributionStats(
        val total: Int,
        val yearly: Int,
        val monthly: Int,
        val weekly: Int,
    )

    companion object {
        private const val API_URL = "https://api.github.com"
        private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    }
}
