package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement

/**
 * Renders the icon as a CSS background-image on an absolutely-positioned <a> element.
 * When wowheadItemId > 0 the anchor is interactive (pointer-events:auto), so the
 * user's real mouse events trigger Wowhead's tooltip script via its own event
 * delegation — no synthetic MouseEvents needed.
 */
@Composable
actual fun GearIcon(
    url: String,
    contentDescription: String,
    modifier: Modifier,
    cornerRadius: Dp,
    borderColor: Color,
    borderWidth: Dp,
    wowheadItemId: Int
) {
    val density = LocalDensity.current.density

    val el = remember {
        val overlay = document.getElementById("gear-overlay") ?: document.documentElement!!
        (document.createElement("a") as HTMLAnchorElement).also {
            it.style.cssText =
                "display:none;position:fixed;background-size:cover;background-position:center;"
            overlay.appendChild(it)
        }
    }

    SideEffect {
        el.style.backgroundImage = "url('$url')"
        el.style.borderRadius = "${cornerRadius.value}px"
        if (borderWidth > 0.dp && borderColor != Color.Transparent) {
            val r = (borderColor.red * 255).toInt()
            val g = (borderColor.green * 255).toInt()
            val b = (borderColor.blue * 255).toInt()
            el.style.border = "${borderWidth.value}px solid rgb($r,$g,$b)"
        } else {
            el.style.border = "none"
        }
        if (wowheadItemId > 0) {
            el.href = "https://www.wowhead.com/tbc/item=$wowheadItemId"
            el.setAttribute("data-wowhead", "item=$wowheadItemId&domain=tbc")
            el.target = "_blank"
            el.style.setProperty("pointer-events", "auto")
            el.style.cursor = "pointer"
        } else {
            el.removeAttribute("href")
            el.removeAttribute("data-wowhead")
            el.style.setProperty("pointer-events", "none")
            el.style.cursor = "default"
        }
    }

    DisposableEffect(Unit) {
        onDispose { el.remove() }
    }

    Box(
        modifier = modifier.onGloballyPositioned { layout ->
            val pos = layout.positionInWindow()
            val size = layout.size
            el.style.left = "${(pos.x / density).toInt()}px"
            el.style.top = "${(pos.y / density).toInt()}px"
            el.style.width = "${(size.width / density).toInt()}px"
            el.style.height = "${(size.height / density).toInt()}px"
            el.style.display = "block"
        }
    )
}
