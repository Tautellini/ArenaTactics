package net.tautellini.arenatactics

import kotlinx.browser.window

private var historyPos = 0
private var popCallback: ((Boolean) -> Unit)? = null

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun pushNavigationState(path: String) {
    val base = js("window.__appBase || ''") as String
    historyPos++
    // Store historyPos as the history state so popstate can detect direction.
    window.history.pushState(historyPos, "", base + path)
}

actual fun registerPopCallback(callback: (isBack: Boolean) -> Unit) {
    popCallback = callback
    window.onpopstate = { event ->
        val newPos = (event.state as? Number)?.toInt() ?: 0
        val isBack = newPos < historyPos
        historyPos = newPos
        popCallback?.invoke(isBack)
    }
}

actual fun getInitialPath(): String = window.location.pathname.removeAppBase()
actual fun getCurrentPath(): String = window.location.pathname.removeAppBase()

private fun String.removeAppBase(): String {
    val base = js("window.__appBase || ''") as String
    return if (base.isNotEmpty()) removePrefix(base) else this
}
