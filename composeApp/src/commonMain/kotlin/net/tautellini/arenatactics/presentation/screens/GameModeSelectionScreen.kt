package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeSelectionState
import net.tautellini.arenatactics.presentation.GameModeSelectionViewModel
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun GameModeSelectionScreen(
    viewModel: GameModeSelectionViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            ShieldLogo()

            when (val s = state) {
                is GameModeSelectionState.Loading ->
                    CircularProgressIndicator(color = Accent)
                is GameModeSelectionState.Error ->
                    Text(s.message, color = TextSecondary)
                is GameModeSelectionState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Select your bracket",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            s.modes.forEach { mode ->
                                GameModeTile(mode) {
                                    navigator.push(Screen.CompositionSelection(mode.id))
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Made with love for Kizaru",
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
    }
}

// ─── Shield logo ─────────────────────────────────────────────────────────────

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

@Composable
private fun ShieldLogo() {
    val cinzel = cinzelDecorative()
    val transition = rememberInfiniteTransition(label = "shield-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-x"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp, 250.dp)) {
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

            // Inner border inset
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                "Arena",
                fontFamily = cinzel,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 26.sp,
                letterSpacing = 3.sp
            )
            Text(
                "Tactics",
                fontFamily = cinzel,
                fontWeight = FontWeight.Normal,
                color = Accent,
                fontSize = 16.sp,
                letterSpacing = 5.sp
            )
        }
    }
}

// ─── Game mode tile ───────────────────────────────────────────────────────────

@Composable
private fun GameModeTile(mode: GameMode, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .alpha(if (mode.hasData) 1f else 0.35f)
            .then(if (mode.hasData) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.large(mode.iconName),
            contentDescription = "${mode.name} ${mode.description}",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            text = mode.description,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = mode.name,
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}
