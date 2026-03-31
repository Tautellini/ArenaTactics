package net.tautellini.arenatactics.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.tautellini.arenatactics.auth.AuthService
import net.tautellini.arenatactics.auth.AuthState
import net.tautellini.arenatactics.auth.OAuthProvider

class AuthViewModel(
    private val authService: AuthService
) : ViewModel() {
    val authState: StateFlow<AuthState> = authService.authState

    fun signIn(provider: OAuthProvider) {
        authService.startOAuthFlow(provider)
    }

    fun handleCallback(token: String, refreshToken: String) {
        authService.handleAuthCallback(token, refreshToken)
    }

    fun signOut() {
        authService.signOut()
    }
}
