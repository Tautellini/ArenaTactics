package net.tautellini.arenatactics.auth

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.tautellini.arenatactics.ApiConfig

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.localStorage.getItem(key)")
private external fun lsGet(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => window.localStorage.setItem(key, value)")
private external fun lsSet(key: String, value: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => window.localStorage.removeItem(key)")
private external fun lsRemove(key: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(url) => { window.location.href = url; }")
private external fun navigateTo(url: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(s) => encodeURIComponent(s)")
private external fun encodeUri(s: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Math.floor(Date.now() / 1000)")
private external fun nowSeconds(): Int

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(token) => { try { return JSON.parse(atob(token.split('.')[1])).exp || 0; } catch(e) { return 0; } }")
private external fun getJwtExpiry(token: String): Int

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(token) => { try { return JSON.parse(atob(token.split('.')[1])).name || ''; } catch(e) { return ''; } }")
private external fun getJwtName(token: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(token) => { try { return JSON.parse(atob(token.split('.')[1])).sub || ''; } catch(e) { return ''; } }")
private external fun getJwtSub(token: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.location.origin")
private external fun getOrigin(): String

private const val KEY_TOKEN = "at_token"
private const val KEY_REFRESH = "at_refresh"

actual class AuthService actual constructor() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    actual val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val token = lsGet(KEY_TOKEN)
        if (token != null && !isExpired(token)) {
            val name = getJwtName(token).ifBlank { null }
            val uid = getJwtSub(token)
            _authState.value = AuthState.Authenticated(uid, name, null)
        } else {
            _authState.value = AuthState.NotAuthenticated
        }
    }

    actual fun startOAuthFlow(provider: OAuthProvider) {
        val origin = getOrigin()
        val redirectUri = "$origin/auth/callback"
        val backendUrl = "${ApiConfig.BASE_URL}/auth/oauth/${provider.id}/start?redirect_uri=${encodeUri(redirectUri)}"
        navigateTo(backendUrl)
    }

    actual fun handleAuthCallback(token: String, refreshToken: String) {
        lsSet(KEY_TOKEN, token)
        lsSet(KEY_REFRESH, refreshToken)
        val name = getJwtName(token).ifBlank { null }
        val uid = getJwtSub(token)
        _authState.value = AuthState.Authenticated(uid, name, null)
    }

    actual suspend fun getAuthHeader(): String? {
        val token = lsGet(KEY_TOKEN) ?: return null
        if (isExpired(token)) {
            signOut() // Simplified — no refresh in WasmJS for now
            return null
        }
        return "Bearer $token"
    }

    actual fun signOut() {
        lsRemove(KEY_TOKEN)
        lsRemove(KEY_REFRESH)
        _authState.value = AuthState.NotAuthenticated
    }

    private fun isExpired(token: String): Boolean {
        val exp = getJwtExpiry(token)
        val now = nowSeconds()
        return now >= exp - 60
    }
}
