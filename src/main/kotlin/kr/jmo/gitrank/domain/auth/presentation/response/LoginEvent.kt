package kr.jmo.gitrank.domain.auth.presentation.response

sealed class LoginEvent(
    val type: String,
) {
    data class Progress(
        val step: LoginStep,
    ) : LoginEvent("progress")

    data class Complete(
        val data: TokenResponse,
    ) : LoginEvent("complete")

    data class Error(
        val error: String,
    ) : LoginEvent("error")
}
