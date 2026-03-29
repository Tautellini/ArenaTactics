package net.tautellini.arenatactics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Wowhead tooltip config
    val cfg = document.createElement("script")
    cfg.textContent = "const whTooltips = {colorLinks:false, iconizeLinks:false, renameLinks:false};"
    document.head!!.appendChild(cfg)

    // Wowhead tooltip script
    val tooltipScript = document.createElement("script")
    tooltipScript.setAttribute("src", "https://wow.zamimg.com/js/tooltips.js")
    document.head!!.appendChild(tooltipScript)

    // Mount Compose into a div with an explicit low z-index instead of document.body.
    // This makes the shadow root canvas a stacking context at z-index:0, so any
    // positioned element in document.body with z-index >= 1 (including Wowhead
    // tooltip divs and our gear icon <a> elements) naturally paints above it.
    // No MutationObserver or gear-overlay workaround needed.
    val composeRoot = document.createElement("div") as HTMLElement
    composeRoot.style.cssText = "position:fixed;top:0;left:0;width:100%;height:100%;z-index:0;"
    document.body!!.appendChild(composeRoot)

    // popstate wiring handled inside App.kt via registerPopCallback + DisposableEffect

    ComposeViewport(composeRoot) {
        App()
    }
}
