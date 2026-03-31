package net.tautellini.arenatactics.auth

import kotlinx.coroutines.flow.StateFlow

expect class AuthService() {
    val authState: StateFlow<AuthState>

    /** Opens the OAuth flow for the given provider (redirects browser or opens system browser). */
    fun startOAuthFlow(provider: OAuthProvider)

    /** Called after OAuth redirect — stores tokens and updates auth state. */
    fun handleAuthCallback(token: String, refreshToken: String)

    /** Returns "Bearer <jwt>" if authenticated and token is valid, or null. */
    suspend fun getAuthHeader(): String?

    /** Clears stored tokens and sets state to NotAuthenticated. */
    fun signOut()
}
