package net.tautellini.arenatactics

import kotlinx.browser.window

@OptIn(ExperimentalWasmJsInterop::class)
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

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(id, x, y) => {
    var el = document.getElementById('wh-tt');
    if (!el) { el = document.createElement('a'); el.id = 'wh-tt'; document.body.appendChild(el); }
    el.href = 'https://www.wowhead.com/tbc/item=' + id;
    el.setAttribute('data-wowhead', 'item=' + id + '&domain=tbc');
    el.style.cssText = 'position:fixed;left:' + x + 'px;top:' + y + 'px;width:1px;height:1px;opacity:0;pointer-events:none;';
    if (window.WH) WH.refreshLinks();
    el.dispatchEvent(new MouseEvent('mouseover', {bubbles:true}));
}""")
private external fun showWowheadTooltipJs(id: Int, x: Float, y: Float)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""() => {
    var el = document.getElementById('wh-tt');
    if (el) el.dispatchEvent(new MouseEvent('mouseout', {bubbles:true}));
}""")
private external fun hideWowheadTooltipJs()

actual fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float) =
    showWowheadTooltipJs(itemId, cursorX, cursorY)

actual fun hideWowheadTooltip() = hideWowheadTooltipJs()
