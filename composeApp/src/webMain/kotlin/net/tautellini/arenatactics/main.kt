package net.tautellini.arenatactics

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

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

    // Ensure Wowhead tooltip popup renders above the Compose canvas
    val style = document.createElement("style")
    style.textContent = "#wowhead-tooltip { z-index: 10000 !important; }"
    document.head!!.appendChild(style)

    // popstate wiring handled inside App.kt via registerPopCallback + DisposableEffect

    ComposeViewport(document.body!!) {
        App()
    }
}
