package net.tautellini.arenatactics

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement

private var popCallback: (() -> Unit)? = null

actual fun refreshWowheadTooltips() {
    js("if (typeof WH !== 'undefined' && typeof WH.refreshLinks === 'function') WH.refreshLinks()")
}

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

private var whTooltipEl: HTMLAnchorElement? = null

actual fun showWowheadTooltip(itemId: Int, cursorX: Float, cursorY: Float) {
    val el = whTooltipEl ?: run {
        val a = document.createElement("a") as HTMLAnchorElement
        a.id = "wh-tt"
        document.body!!.appendChild(a)
        whTooltipEl = a
        a
    }
    el.href = "https://www.wowhead.com/tbc/item=$itemId"
    el.setAttribute("data-wowhead", "item=$itemId&domain=tbc")
    el.style.cssText =
        "position:fixed;left:${cursorX}px;top:${cursorY}px;width:1px;height:1px;opacity:0;pointer-events:none;"
    js("if (window.WH && typeof WH.refreshLinks === 'function') WH.refreshLinks()")
    js("var e=document.getElementById('wh-tt'); if(e) e.dispatchEvent(new MouseEvent('mouseover',{bubbles:true,view:window,relatedTarget:document.body}))")
}

actual fun hideWowheadTooltip() {
    js("var e=document.getElementById('wh-tt'); if(e) e.dispatchEvent(new MouseEvent('mouseout',{bubbles:true,view:window,relatedTarget:document.body}))")
}

actual fun observeWowheadTooltips() {
    js("""
        var overlay = document.getElementById('gear-overlay');
        if (!overlay) return;
        new MutationObserver(function(mutations) {
            mutations.forEach(function(m) {
                m.addedNodes.forEach(function(n) {
                    if (n.nodeType === 1 && n.id && n.id.indexOf('wowhead') >= 0) {
                        overlay.appendChild(n);
                    }
                });
            });
        }).observe(document.body, { childList: true });
    """)
}
