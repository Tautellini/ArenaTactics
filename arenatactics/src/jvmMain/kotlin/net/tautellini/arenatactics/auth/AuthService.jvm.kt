package net.tautellini.arenatactics.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Desktop
import java.net.URI

actual class AuthService actual constructor() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    actual val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // In-memory token storage for desktop
    private var token: String? = null
    private var refreshToken: String? = null

    actual fun startOAuthFlow(provider: OAuthProvider) {
        // Desktop: open system browser for OAuth
        // TODO: implement localhost redirect capture for desktop OAuth
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(URI("https://backend-532845578761.europe-west3.run.app/auth/oauth/${provider.id}/start?redirect_uri=http://localhost:9274/callback"))
            } catch (_: Exception) {}
        }
    }

    actual fun handleAuthCallback(token: String, refreshToken: String) {
        this.token = token
        this.refreshToken = refreshToken
        // Parse JWT payload manually
        try {
            val parts = token.split(".")
            if (parts.size >= 2) {
                val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                val uid = Regex("\"sub\":\"([^\"]+)\"").find(payload)?.groupValues?.get(1) ?: ""
                val name = Regex("\"name\":\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
                _authState.value = AuthState.Authenticated(uid, name, null)
            }
        } catch (_: Exception) {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    actual suspend fun getAuthHeader(): String? {
        return token?.let { "Bearer $it" }
    }

    actual fun signOut() {
        token = null
        refreshToken = null
        _authState.value = AuthState.NotAuthenticated
    }
}
