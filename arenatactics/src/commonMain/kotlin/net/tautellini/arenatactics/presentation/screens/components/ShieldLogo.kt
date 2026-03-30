package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.presentation.theme.*

/**
 * Arena Tactics logo: two crossed swords behind a hexagonal shield
 * with a diamond/chevron emblem inside.
 *
 * The caller provides sizing via [modifier]. Typical sizes:
 * - Sidebar brand: `Modifier.size(48.dp, 56.dp)`
 * - Dashboard/large: `Modifier.size(80.dp, 94.dp)`
 */
@Composable
internal fun ShieldCanvas(modifier: Modifier = Modifier, shimmerX: Float = -1f) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // ── Swords (behind shield) ──
        drawSword(cx, cy, rotationDegrees = -30f)
        drawSword(cx, cy, rotationDegrees = 30f)

        // ── Shield ──
        val shieldPath = hexShieldPath(cx, cy, w * 0.44f, h * 0.38f)

        // Shield fill
        drawPath(
            shieldPath,
            brush = Brush.verticalGradient(
                listOf(Surface, BgDeep),
                startY = cy - h * 0.35f,
                endY = cy + h * 0.35f
            )
        )

        // Shield inner highlight
        drawPath(
            shieldPath,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                startY = cy - h * 0.35f,
                endY = cy
            )
        )

        // Shield border
        drawPath(
            shieldPath,
            brush = Brush.verticalGradient(
                listOf(Primary, Primary.copy(alpha = 0.15f)),
                startY = cy - h * 0.35f,
                endY = cy + h * 0.35f
            ),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // ── Inner border (subtle) ──
        val innerShield = hexShieldPath(cx, cy, w * 0.38f, h * 0.32f)
        drawPath(
            innerShield,
            color = Primary.copy(alpha = 0.12f),
            style = Stroke(width = 0.7.dp.toPx())
        )

        // ── Center diamond emblem ──
        val diamondOuter = Path().apply {
            moveTo(cx, cy - h * 0.11f)
            lineTo(cx + w * 0.10f, cy)
            lineTo(cx, cy + h * 0.11f)
            lineTo(cx - w * 0.10f, cy)
            close()
        }
        drawPath(
            diamondOuter,
            color = Primary.copy(alpha = 0.35f),
            style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Miter)
        )

        val diamondInner = Path().apply {
            moveTo(cx, cy - h * 0.07f)
            lineTo(cx + w * 0.065f, cy)
            lineTo(cx, cy + h * 0.07f)
            lineTo(cx - w * 0.065f, cy)
            close()
        }
        drawPath(
            diamondInner,
            color = Primary.copy(alpha = 0.1f)
        )

        // Shimmer sweep (if animated)
        if (shimmerX > -0.9f) {
            val sx = shimmerX * w
            drawPath(
                shieldPath,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.4f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.12f),
                        0.6f to Color.Transparent,
                        1.0f to Color.Transparent
                    ),
                    start = Offset(sx, 0f),
                    end = Offset(sx + w, h)
                )
            )
        }
    }
}

/**
 * Draws a single sword centered at (cx, cy), rotated by [rotationDegrees].
 */
private fun DrawScope.drawSword(cx: Float, cy: Float, rotationDegrees: Float) {
    val w = size.width
    val h = size.height

    rotate(rotationDegrees, pivot = Offset(cx, cy)) {
        val bladeW = w * 0.055f
        val bladeH = h * 0.50f
        val bladeTop = cy - bladeH * 0.55f
        val bladeBottom = bladeTop + bladeH

        // Blade
        val bladePath = Path().apply {
            moveTo(cx - bladeW / 2f, bladeBottom)
            lineTo(cx - bladeW / 2f, bladeTop + bladeW)
            lineTo(cx, bladeTop) // pointed tip
            lineTo(cx + bladeW / 2f, bladeTop + bladeW)
            lineTo(cx + bladeW / 2f, bladeBottom)
            close()
        }

        val bladeBrush = Brush.verticalGradient(
            listOf(PrimaryBright, TextSecondary, TextMuted),
            startY = bladeTop,
            endY = bladeBottom
        )
        drawPath(bladePath, bladeBrush)

        // Crossguard
        val guardY = bladeBottom
        val guardW = w * 0.12f
        val guardH = h * 0.04f
        drawRoundRect(
            color = Primary,
            topLeft = Offset(cx - guardW, guardY),
            size = androidx.compose.ui.geometry.Size(guardW * 2f, guardH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        )

        // Grip
        val gripTop = guardY + guardH
        val gripH = h * 0.12f
        val gripW = bladeW * 0.85f
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(BgStone, BgDeep),
                startY = gripTop,
                endY = gripTop + gripH
            ),
            topLeft = Offset(cx - gripW / 2f, gripTop),
            size = androidx.compose.ui.geometry.Size(gripW, gripH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
        )

        // Pommel
        val pommelY = gripTop + gripH + h * 0.01f
        drawCircle(
            color = Primary.copy(alpha = 0.5f),
            radius = w * 0.025f,
            center = Offset(cx, pommelY + w * 0.025f)
        )
    }
}

/**
 * Creates a pointed hexagonal shield path centered at (cx, cy).
 */
private fun hexShieldPath(cx: Float, cy: Float, halfW: Float, halfH: Float): Path = Path().apply {
    // Top center → top right → mid right → bottom point → mid left → top left
    moveTo(cx, cy - halfH)              // top center
    lineTo(cx + halfW, cy - halfH * 0.65f) // top right
    lineTo(cx + halfW, cy + halfH * 0.25f) // mid right
    lineTo(cx, cy + halfH)              // bottom point
    lineTo(cx - halfW, cy + halfH * 0.25f) // mid left
    lineTo(cx - halfW, cy - halfH * 0.65f) // top left
    close()
}
