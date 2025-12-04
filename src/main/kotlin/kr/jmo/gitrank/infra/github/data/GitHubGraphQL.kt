package kr.jmo.gitrank.infra.github.data

data class GraphQLResponse<T>(
    val data: T?,
)

data class ViewerResponse(
    val viewer: Viewer?,
)

data class Viewer(
    val contributionsCollection: ContributionsCollection?,
)

data class ContributionsCollection(
    val contributionYears: List<Int>?,
    val contributionCalendar: ContributionCalendar?,
)

data class ContributionCalendar(
    val totalContributions: Int?,
    val weeks: List<Week>?,
)

data class Week(
    val contributionDays: List<ContributionDay>?,
)

data class ContributionDay(
    val contributionCount: Int?,
    val date: String?,
)
