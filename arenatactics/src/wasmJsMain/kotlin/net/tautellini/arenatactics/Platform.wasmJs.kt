package net.tautellini.arenatactics

import kotlinx.browser.window

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.__appBase || ''")
private external fun getAppBase(): String

/**
 * Pushes a history entry with the position counter as the state value so
 * popstate can detect navigation direction by comparing the state to our
 * Kotlin-side counter.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(pos, path) => window.history.pushState(pos, '', (window.__appBase || '') + path)")
private external fun jsPushState(pos: Int, path: String)

/**
 * Reads the numeric state from a PopStateEvent. Returns 0 if absent.
 * In WasmJs the event lambda receives no typed argument, so we read the
 * state from window.history.state immediately after the event fires —
 * that property is already updated to the new entry's state by the time
 * our onpopstate handler runs.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => { const s = window.history.state; return (typeof s === 'number') ? s : 0; }")
private external fun jsReadCurrentHistoryState(): Int

private var historyPos = 0
private var popCallback: ((Boolean) -> Unit)? = null

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun pushNavigationState(path: String) {
    historyPos++
    jsPushState(historyPos, path)
}

actual fun registerPopCallback(callback: (isBack: Boolean) -> Unit) {
    popCallback = callback
    window.onpopstate = {
        val newPos = jsReadCurrentHistoryState()
        val isBack = newPos < historyPos
        historyPos = newPos
        popCallback?.invoke(isBack)
    }
}

actual fun getInitialPath(): String = window.location.pathname.removeAppBase()
actual fun getCurrentPath(): String = window.location.pathname.removeAppBase()

private fun String.removeAppBase(): String {
    val base = getAppBase()
    return if (base.isNotEmpty()) removePrefix(base) else this
}
