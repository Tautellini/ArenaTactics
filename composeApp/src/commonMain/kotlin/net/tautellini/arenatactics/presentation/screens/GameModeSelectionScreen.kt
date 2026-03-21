package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeSelectionState
import net.tautellini.arenatactics.presentation.GameModeSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.ShieldCanvas
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun GameModeSelectionScreen(
    viewModel: GameModeSelectionViewModel,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier
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
            ShieldLogo(modifier = shieldModifier)

            when (val s = state) {
                is GameModeSelectionState.Loading ->
                    CircularProgressIndicator(color = Primary)
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
                                    onNavigate(Screen.CompositionSelection(mode.id))
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

@Composable
private fun ShieldLogo(modifier: Modifier = Modifier) {
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
        ShieldCanvas(
            modifier = modifier.size(220.dp, 250.dp),
            shimmerX = shimmerX
        )

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
                color = Primary,
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
