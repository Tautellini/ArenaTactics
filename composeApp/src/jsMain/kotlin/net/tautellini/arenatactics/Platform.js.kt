package net.tautellini.arenatactics

import kotlinx.browser.document
import kotlinx.browser.window

private var popCallback: (() -> Unit)? = null

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun pushNavigationState(path: String) {
    val base = js("window.__appBase || ''") as String
    window.history.pushState(null, "", base + path)
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    window.onpopstate = { popCallback?.invoke() }
}
actual fun getInitialPath(): String = window.location.pathname

