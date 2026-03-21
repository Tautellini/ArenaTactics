package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeRowState
import net.tautellini.arenatactics.presentation.HomeState
import net.tautellini.arenatactics.presentation.HomeViewModel
import net.tautellini.arenatactics.presentation.screens.components.ShieldCanvas
import net.tautellini.arenatactics.presentation.theme.*

private data class HomeSelection(
    val addon: Addon? = null,
    val section: Section? = null
)

private enum class Section { TACTICS, CLASS_GUIDES }

@Composable
fun AddonSelectionScreen(
    viewModel: HomeViewModel,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var selection by remember { mutableStateOf(HomeSelection()) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            ShieldLogoBlock(modifier = shieldModifier)

            when (val s = state) {
                is HomeState.Loading -> CircularProgressIndicator(color = Primary)
                is HomeState.Error -> Text(s.message, color = TextSecondary)
                is HomeState.Success -> {
                    // Row 1: Addon selection — always visible
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Select your game",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            s.addons.forEach { addon ->
                                val isSelected = selection.addon?.id == addon.id
                                val isDone = selection.section != null
                                val tileAlpha = when {
                                    !addon.hasData -> 0.35f
                                    isSelected -> 1f
                                    isDone -> 0.6f
                                    else -> 1f
                                }
                                AddonTile(
                                    addon = addon,
                                    isSelected = isSelected,
                                    alpha = tileAlpha,
                                    onClick = if (!addon.hasData) null else ({
                                        if (isSelected) {
                                            // Deselect
                                            selection = HomeSelection()
                                            viewModel.resetGameModes()
                                        } else {
                                            // Select new addon
                                            viewModel.resetGameModes()
                                            selection = HomeSelection(addon = addon)
                                            viewModel.loadGameModes(addon.id)
                                        }
                                    })
                                )
                            }
                        }
                    }

                    // Row 2: Section selection — visible when addon is selected
                    AnimatedVisibility(
                        visible = selection.addon != null,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "What are you looking for?",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val tacticsEnabled = isTacticsEnabled(s.gameModeRow)
                                val tacticsAlpha = if (tacticsEnabled) 1f else 0.35f

                                SectionTile(
                                    icon = Icons.Rounded.AutoAwesome,
                                    title = "Tactics",
                                    subtitle = "Compositions & matchup guides",
                                    isSelected = selection.section == Section.TACTICS,
                                    alpha = tacticsAlpha,
                                    loadingState = s.gameModeRow,
                                    onClick = if (tacticsEnabled) ({
                                        selection = selection.copy(section = Section.TACTICS)
                                    }) else null
                                )
                                SectionTile(
                                    icon = Icons.Rounded.MenuBook,
                                    title = "Class Guides",
                                    subtitle = "Best-in-slot gear per spec",
                                    isSelected = selection.section == Section.CLASS_GUIDES,
                                    alpha = 1f,
                                    loadingState = null,
                                    onClick = {
                                        selection.addon?.let { addon ->
                                            onNavigate(Screen.ClassGuideList(addon.id))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Row 3: Bracket selection — visible when Tactics is selected
                    AnimatedVisibility(
                        visible = selection.section == Section.TACTICS,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        val gameModeRow = s.gameModeRow
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
                            if (gameModeRow is GameModeRowState.Ready) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    gameModeRow.modes.forEach { mode ->
                                        GameModeTile(mode) {
                                            selection.addon?.let { addon ->
                                                onNavigate(Screen.CompositionSelection(addon.id, mode.id))
                                            }
                                        }
                                    }
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

/** Tactics is enabled when game modes are Ready and at least one has data. */
private fun isTacticsEnabled(gameModeRow: GameModeRowState): Boolean =
    gameModeRow is GameModeRowState.Ready && gameModeRow.modes.any { it.hasData }

@Composable
private fun ShieldLogoBlock(modifier: Modifier = Modifier) {
    val cinzel = cinzelDecorative()
    val transition = rememberInfiniteTransition(label = "shield-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-x"
    )
    Box(contentAlignment = Alignment.Center) {
        ShieldCanvas(modifier = modifier.size(220.dp, 250.dp), shimmerX = shimmerX)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text("Arena", fontFamily = cinzel, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 26.sp, letterSpacing = 3.sp)
            Text("Tactics", fontFamily = cinzel, fontWeight = FontWeight.Normal, color = Primary, fontSize = 16.sp, letterSpacing = 5.sp)
        }
    }
}

@Composable
private fun AddonTile(
    addon: Addon,
    isSelected: Boolean,
    alpha: Float,
    onClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .alpha(alpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                if (isSelected) Modifier.border(2.dp, Primary, shape).background(CardElevated, shape)
                else Modifier
            )
            .padding(8.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.large(addon.iconName),
            contentDescription = addon.name,
            modifier = Modifier.size(80.dp).clip(shape)
        )
        Text(addon.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(addon.description, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SectionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    alpha: Float,
    loadingState: GameModeRowState?,
    onClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(16.dp)
    val borderMod = if (isSelected) Modifier.border(2.dp, Primary, shape) else Modifier
    Surface(
        color = if (isSelected) CardElevated else CardColor,
        shape = shape,
        modifier = Modifier
            .widthIn(min = 160.dp, max = 280.dp)
            .then(borderMod)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(alpha)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (loadingState) {
                is GameModeRowState.Loading -> CircularProgressIndicator(
                    color = Primary,
                    modifier = Modifier.size(40.dp)
                )
                is GameModeRowState.Error -> Text(
                    "Failed to load",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                else -> Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
            }
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

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
            contentDescription = mode.name,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
        )
        Text(mode.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(mode.name, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}
