package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import org.w3c.dom.HTMLImageElement

@Composable
actual fun GearIcon(
    url: String,
    contentDescription: String,
    modifier: Modifier,
    cornerRadius: Dp,
    borderColor: Color,
    borderWidth: Dp
) {
    val density = LocalDensity.current.density

    val img = androidx.compose.runtime.remember {
        (document.createElement("img") as HTMLImageElement).also {
            it.style.cssText = "position:fixed;display:none;pointer-events:none;object-fit:cover;"
            document.body!!.appendChild(it)
        }
    }

    SideEffect {
        img.src = url
        img.alt = contentDescription
        img.style.borderRadius = "${cornerRadius.value}px"
        if (borderWidth > 0.dp && borderColor != Color.Transparent) {
            val r = (borderColor.red * 255).toInt()
            val g = (borderColor.green * 255).toInt()
            val b = (borderColor.blue * 255).toInt()
            img.style.border = "${borderWidth.value}px solid rgb($r,$g,$b)"
        } else {
            img.style.border = "none"
        }
    }

    DisposableEffect(Unit) {
        onDispose { img.remove() }
    }

    Box(
        modifier = modifier.onGloballyPositioned { layout ->
            val pos = layout.positionInWindow()
            val size = layout.size
            img.style.left = "${(pos.x / density).toInt()}px"
            img.style.top = "${(pos.y / density).toInt()}px"
            img.style.width = "${(size.width / density).toInt()}px"
            img.style.height = "${(size.height / density).toInt()}px"
            img.style.display = "block"
        }
    )
}
