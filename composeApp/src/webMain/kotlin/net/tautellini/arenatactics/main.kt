package net.tautellini.arenatactics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Wowhead tooltip config — disable auto-scanning of page links;
    // we trigger tooltips programmatically via showWowheadTooltip().
    val cfg = document.createElement("script")
    cfg.textContent = "const whTooltips = {colorLinks:false, iconizeLinks:false, renameLinks:false};"
    document.head!!.appendChild(cfg)

    // Wowhead tooltip script (current API — tooltips.js)
    val tooltipScript = document.createElement("script")
    tooltipScript.setAttribute("src", "https://wow.zamimg.com/js/tooltips.js")
    document.head!!.appendChild(tooltipScript)

    // popstate wiring handled inside App.kt via registerPopCallback + DisposableEffect

    ComposeViewport(document.body!!) {
        App()
    }
}
