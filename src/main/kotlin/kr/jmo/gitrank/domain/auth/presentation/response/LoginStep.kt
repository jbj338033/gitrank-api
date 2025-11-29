package kr.jmo.gitrank.domain.auth.presentation.response

enum class LoginStep(val value: String) {
    AUTHENTICATING("authenticating"),
    SYNCING("syncing"),
    ;

    override fun toString(): String = value
}
