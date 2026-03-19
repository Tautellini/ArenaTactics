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

    // Overlay layer appended to <html> (outside <body>/shadow root) so it
    // always paints above the Compose canvas regardless of z-index.
    val overlay = document.createElement("div")
    overlay.id = "gear-overlay"
    overlay.setAttribute(
        "style",
        "position:fixed;top:0;left:0;width:0;height:0;pointer-events:none;"
    )
    document.documentElement!!.appendChild(overlay)

    // Reparent Wowhead tooltip divs from body into the overlay as they are created,
    // so they paint above the Compose canvas (which lives in body's shadow root).
    observeWowheadTooltips()

    // popstate wiring handled inside App.kt via registerPopCallback + DisposableEffect

    ComposeViewport(document.body!!) {
        App()
    }
}
