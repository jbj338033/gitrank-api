package kr.gitrank.api.infra.github.dto

data class GraphQLResponse<T>(
    val data: T?
)

data class ViewerResponse(
    val viewer: Viewer?
)

data class Viewer(
    val contributionsCollection: ContributionsCollection?
)

data class ContributionsCollection(
    val contributionYears: List<Int>?,
    val contributionCalendar: ContributionCalendar?
)

data class ContributionCalendar(
    val totalContributions: Int?
)
