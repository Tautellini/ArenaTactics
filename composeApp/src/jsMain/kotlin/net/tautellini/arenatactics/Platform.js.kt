package net.tautellini.arenatactics

import kotlinx.browser.window

private var popCallback: (() -> Unit)? = null

actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun pushNavigationState(path: String) {
    window.history.pushState(null, "", path)
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    js("window.onpopstate = function() { net.tautellini.arenatactics.invokePopCallback() }")
}

internal fun invokePopCallback() { popCallback?.invoke() }
