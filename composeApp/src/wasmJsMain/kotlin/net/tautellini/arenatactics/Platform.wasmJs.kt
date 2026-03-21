package net.tautellini.arenatactics

import kotlinx.browser.window

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.__appBase || ''")
private external fun getAppBase(): String

private var popCallback: (() -> Unit)? = null

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

@OptIn(ExperimentalWasmJsInterop::class)
actual fun pushNavigationState(path: String) {
    window.history.pushState(null, "", getAppBase() + path)
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    window.onpopstate = { popCallback?.invoke() }
}
actual fun getInitialPath(): String = window.location.pathname
actual fun getCurrentPath(): String = window.location.pathname

