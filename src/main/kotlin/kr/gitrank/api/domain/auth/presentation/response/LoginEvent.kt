package kr.gitrank.api.domain.auth.presentation.response

data class LoginEvent(
    val type: String,
    val step: String? = null,
    val data: TokenResponse? = null,
    val error: String? = null
) {
    companion object {
        fun progress(step: String) = LoginEvent(type = "progress", step = step)
        fun complete(data: TokenResponse) = LoginEvent(type = "complete", data = data)
        fun error(message: String) = LoginEvent(type = "error", error = message)
    }
}
