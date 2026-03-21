package net.tautellini.arenatactics.presentation.screens

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
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun TacticsGameModeSelectionScreen(
    addonId: String,
    viewModel: GameModeSelectionViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Select your bracket", color = TextSecondary, fontSize = 13.sp, letterSpacing = 2.sp)
            when (val s = state) {
                is GameModeSelectionState.Loading -> CircularProgressIndicator(color = Primary)
                is GameModeSelectionState.Error   -> Text(s.message, color = TextSecondary)
                is GameModeSelectionState.Success -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        s.modes.forEach { mode ->
                            GameModeTileT7(mode) {
                                onNavigate(Screen.CompositionSelection(addonId, mode.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameModeTileT7(mode: GameMode, onClick: () -> Unit) {
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
            contentDescription = mode.description,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(mode.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(mode.name, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
