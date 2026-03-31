package net.tautellini.arenatactics.auth

enum class OAuthProvider(val id: String, val label: String) {
    GOOGLE("google", "Google"),
    DISCORD("discord", "Discord"),
    BATTLENET("battlenet", "Battle.net")
}

sealed class AuthState {
    data object Loading : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(
        val uid: String,
        val name: String?,
        val avatarUrl: String?
    ) : AuthState()
}
