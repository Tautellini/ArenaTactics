package net.tautellini.arenatactics.auth

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tautellini.arenatactics.ApiConfig
import org.w3c.fetch.RequestInit
import kotlin.js.json

private const val KEY_TOKEN = "at_token"
private const val KEY_REFRESH = "at_refresh"

actual class AuthService actual constructor() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    actual val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val token = window.localStorage.getItem(KEY_TOKEN)
        if (token != null && !isExpired(token)) {
            val payload = parseJwt(token)
            _authState.value = AuthState.Authenticated(
                uid = payload["sub"] as? String ?: "",
                name = payload["name"] as? String,
                avatarUrl = null
            )
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    actual fun startOAuthFlow(provider: OAuthProvider) {
        val appBase = js("window.__appBase || ''") as String
        val redirectUri = "${window.location.origin}$appBase/auth/callback"
        val encoded = js("encodeURIComponent")(redirectUri) as String
        val backendUrl = "${ApiConfig.BASE_URL}/auth/oauth/${provider.id}/start?redirect_uri=$encoded"
        window.location.href = backendUrl
    }

    actual fun handleAuthCallback(token: String, refreshToken: String) {
        window.localStorage.setItem(KEY_TOKEN, token)
        window.localStorage.setItem(KEY_REFRESH, refreshToken)
        val payload = parseJwt(token)
        _authState.value = AuthState.Authenticated(
            uid = payload["sub"] as? String ?: "",
            name = payload["name"] as? String,
            avatarUrl = null
        )
    }

    actual suspend fun getAuthHeader(): String? {
        val token = window.localStorage.getItem(KEY_TOKEN) ?: return null
        if (isExpired(token)) {
            if (!refreshToken()) {
                signOut()
                return null
            }
            val newToken = window.localStorage.getItem(KEY_TOKEN) ?: return null
            return "Bearer $newToken"
        }
        return "Bearer $token"
    }

    actual fun signOut() {
        window.localStorage.removeItem(KEY_TOKEN)
        window.localStorage.removeItem(KEY_REFRESH)
        _authState.value = AuthState.NotAuthenticated
    }

    private fun isExpired(token: String): Boolean {
        val payload = parseJwt(token)
        val exp = (payload["exp"] as? Number)?.toLong() ?: return true
        val now = (js("Math.floor(Date.now() / 1000)") as Number).toLong()
        return now >= exp - 60
    }

    private fun parseJwt(token: String): dynamic {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return js("{}")
            val payload = parts[1]
            val decoded = js("atob")(payload) as String
            JSON.parse<dynamic>(decoded)
        } catch (_: Throwable) {
            js("{}")
        }
    }

    private suspend fun refreshToken(): Boolean {
        val refresh = window.localStorage.getItem(KEY_REFRESH) ?: return false
        return try {
            val body = JSON.stringify(json("refreshToken" to refresh))
            val response = window.fetch(
                "${ApiConfig.BASE_URL}/auth/refresh",
                RequestInit(method = "POST", headers = json("Content-Type" to "application/json"), body = body)
            ).await()

            if (response.ok) {
                val text = response.text().await()
                val data = JSON.parse<dynamic>(text)
                val newToken = data.token as? String
                val newRefresh = data.refreshToken as? String
                if (newToken != null && newRefresh != null) {
                    window.localStorage.setItem(KEY_TOKEN, newToken)
                    window.localStorage.setItem(KEY_REFRESH, newRefresh)
                    val payload = parseJwt(newToken)
                    _authState.value = AuthState.Authenticated(
                        uid = payload.sub as? String ?: "",
                        name = payload.name as? String,
                        avatarUrl = null
                    )
                    true
                } else false
            } else false
        } catch (_: Throwable) {
            false
        }
    }
}
