package net.tautellini.arenatactics

import kotlin.js.JsFun
import kotlinx.browser.window

@JsFun("() => window.__appBase || ''")
private external fun getAppBase(): String

private var popCallback: (() -> Unit)? = null

actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun pushNavigationState(path: String) {
    window.history.pushState(null, "", getAppBase() + path)
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    window.onpopstate = { popCallback?.invoke() }
}
actual fun getInitialPath(): String = window.location.pathname
