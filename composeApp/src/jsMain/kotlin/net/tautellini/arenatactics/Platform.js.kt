package net.tautellini.arenatactics

private var popCallback: (() -> Unit)? = null

actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined') WH.refreshLinks()")
}

actual fun openUrl(url: String) {
    js("window.open(url, '_blank')")
}

actual fun pushNavigationState(path: String) {
    js("history.pushState(null, '', path)")
}

actual fun registerPopCallback(callback: () -> Unit) {
    popCallback = callback
    js("window.onpopstate = function() { net.tautellini.arenatactics.invokePopCallback() }")
}

fun invokePopCallback() { popCallback?.invoke() }
