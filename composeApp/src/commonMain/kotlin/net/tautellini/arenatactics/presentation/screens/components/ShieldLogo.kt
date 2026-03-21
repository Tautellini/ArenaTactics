package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.presentation.theme.Accent
import net.tautellini.arenatactics.presentation.theme.CardColor
import net.tautellini.arenatactics.presentation.theme.Surface

private fun DrawScope.shieldPath(w: Float, h: Float): Path = Path().apply {
    val r = w * 0.08f
    moveTo(r, 0f)
    lineTo(w - r, 0f)
    quadraticTo(w, 0f, w, r)
    lineTo(w, h * 0.58f)
    cubicTo(w, h * 0.82f, w * 0.5f, h, w * 0.5f, h)
    cubicTo(0f, h * 0.82f, 0f, h * 0.58f, 0f, h * 0.58f)
    lineTo(0f, r)
    quadraticTo(0f, 0f, r, 0f)
    close()
}

/**
 * Draws the heraldic shield canvas.
 *
 * The caller MUST provide sizing via [modifier] (e.g. `Modifier.size(220.dp, 250.dp)` for
 * the home screen, `Modifier.size(28.dp, 32.dp)` for AppHeader). The caller also injects
 * any `sharedElement()` modifier here so the shield can participate in shared transitions.
 *
 * [shimmerX] is an animated float (-1f..2f) driving the shimmer sweep. Pass -1f for no shimmer.
 */
@Composable
internal fun ShieldCanvas(modifier: Modifier = Modifier, shimmerX: Float = -1f) {
    Canvas(modifier = modifier) {
        val path = shieldPath(size.width, size.height)

        // Base fill — vertical gradient surface → card
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(Surface, CardColor),
                startY = 0f,
                endY = size.height
            )
        )

        // Inner highlight — subtle lighter edge at top
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.4f
            )
        )

        // Shimmer sweep
        val sx = shimmerX * size.width
        drawPath(
            path,
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.4f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.18f),
                    0.6f to Color.Transparent,
                    1.0f to Color.Transparent
                ),
                start = Offset(sx, 0f),
                end = Offset(sx + size.width, size.height)
            )
        )

        // Border
        drawPath(
            path,
            color = Accent,
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Inner inset border
        val inset = 6.dp.toPx()
        val innerPath = Path().apply {
            val r = (size.width * 0.08f) - inset
            moveTo(inset + r, inset)
            lineTo(size.width - inset - r, inset)
            quadraticTo(size.width - inset, inset, size.width - inset, inset + r)
            lineTo(size.width - inset, size.height * 0.56f)
            cubicTo(
                size.width - inset, size.height * 0.81f,
                size.width * 0.5f, size.height - inset,
                size.width * 0.5f, size.height - inset
            )
            cubicTo(
                inset, size.height * 0.81f,
                inset, size.height * 0.56f,
                inset, size.height * 0.56f
            )
            lineTo(inset, inset + r)
            quadraticTo(inset, inset, inset + r, inset)
            close()
        }
        drawPath(
            innerPath,
            color = Accent.copy(alpha = 0.25f),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
}
