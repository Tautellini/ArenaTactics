package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.Addon
import net.tautellini.arenatactics.data.model.GameMode
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.GameModeRowState
import net.tautellini.arenatactics.presentation.HomeSection
import net.tautellini.arenatactics.presentation.HomeState
import net.tautellini.arenatactics.presentation.HomeViewModel
import net.tautellini.arenatactics.presentation.theme.CardColor
import net.tautellini.arenatactics.presentation.theme.CardElevated
import net.tautellini.arenatactics.presentation.theme.DividerColor
import net.tautellini.arenatactics.presentation.theme.Primary
import net.tautellini.arenatactics.presentation.theme.Surface
import net.tautellini.arenatactics.presentation.theme.TextPrimary
import net.tautellini.arenatactics.presentation.theme.TextSecondary
import net.tautellini.arenatactics.presentation.theme.cinzelDecorative

// ─── Helpers ────────────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val argb = cleaned.toLong(16) or 0xFF000000
    return Color(argb.toInt())
}

private fun String.formatId(): String =
    replace('-', ' ').replace('_', ' ')
        .split(' ')
        .joinToString(" ") { word ->
            if (word.all { it.isDigit() || it == 'v' }) word.uppercase()
            else word.replaceFirstChar { it.uppercaseChar() }
        }

private fun Screen.extractAddonId(): String? = when (this) {
    is Screen.AddonSelection -> null
    is Screen.CompositionSelection -> addonId
    is Screen.MatchupList -> addonId
    is Screen.MatchupDetail -> addonId
    is Screen.ClassGuideList -> addonId
    is Screen.SpecGuide -> addonId
    is Screen.Ladder -> addonId
    is Screen.PlayerDetail -> addonId
}

private fun Screen.extractGameModeId(): String? = when (this) {
    is Screen.CompositionSelection -> gameModeId
    is Screen.MatchupList -> gameModeId
    is Screen.MatchupDetail -> gameModeId
    else -> null
}

private fun Screen.isTacticsPath(): Boolean = when (this) {
    is Screen.CompositionSelection, is Screen.MatchupList, is Screen.MatchupDetail -> true
    else -> false
}

private fun Screen.isGuidesPath(): Boolean = when (this) {
    is Screen.ClassGuideList, is Screen.SpecGuide -> true
    else -> false
}

// ─── Shared shape ───────────────────────────────────────────────────────────

private val SegmentShape = RoundedCornerShape(10.dp)

// ─── Segment types ──────────────────────────────────────────────────────────

private enum class SegmentType { ADDON, SECTION, BRACKET }

// ─── Main composable ────────────────────────────────────────────────────────

@Composable
fun AppHeader(
    currentScreen: Screen,
    homeViewModel: HomeViewModel,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier  // kept for API compat
) {
    val homeState by homeViewModel.state.collectAsState()
    val addonId = currentScreen.extractAddonId()
    val gameModeId = currentScreen.extractGameModeId()
    val isTactics = currentScreen.isTacticsPath()
    val isGuides = currentScreen.isGuidesPath()
    val isLadder = currentScreen is Screen.Ladder || currentScreen is Screen.PlayerDetail

    // Ensure game modes are loaded for the current addon
    LaunchedEffect(addonId) {
        if (addonId != null) homeViewModel.loadGameModes(addonId)
    }

    val successState = homeState as? HomeState.Success
    val addons = successState?.addons ?: emptyList()
    val gameModes = (successState?.gameModeRow as? GameModeRowState.Ready)?.modes ?: emptyList()
    val currentAddon = addons.find { it.id == addonId }
    val currentGameMode = gameModes.find { it.id == gameModeId }

    var expanded by remember { mutableStateOf<SegmentType?>(null) }

    // Close expansion on screen change
    LaunchedEffect(currentScreen) { expanded = null }

    // Shield shimmer
    val transition = rememberInfiniteTransition(label = "shield-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-x"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        // Shield — click to go home
        Box(modifier = Modifier.clickable {
            expanded = null
            onNavigate(Screen.AddonSelection)
        }) {
            ShieldCanvas(
                modifier = shieldModifier.size(30.dp, 34.dp),
                shimmerX = shimmerX
            )
        }

        Spacer(Modifier.width(12.dp))

        val accentColor = currentAddon?.let { parseHexColor(it.accentColor) } ?: Primary

        // Build the segment list
        val segments = buildList {
            add(SegmentType.ADDON)
            add(SegmentType.SECTION)
            if (isTactics && gameModeId != null) add(SegmentType.BRACKET)
        }

        val expandedIndex = if (expanded != null) segments.indexOf(expanded!!) else -1

        // Segments up to and including the expanded one
        val visibleSegments = if (expandedIndex >= 0) {
            segments.subList(0, expandedIndex + 1)
        } else {
            segments
        }

        // ─── Navigation segments ────────────────────────────────
        visibleSegments.forEachIndexed { index, segment ->
            if (index > 0) Spacer(Modifier.width(6.dp))

            val isExpanded = expanded == segment
            val segInteraction = remember { MutableInteractionSource() }
            val segHovered by segInteraction.collectIsHoveredAsState()

            val borderColor by animateColorAsState(
                when {
                    isExpanded -> Primary
                    segHovered -> Primary.copy(alpha = 0.3f)
                    segment == SegmentType.ADDON -> accentColor.copy(alpha = 0.4f)
                    else -> DividerColor
                },
                animationSpec = tween(200)
            )
            val bgColor by animateColorAsState(
                when {
                    isExpanded -> CardElevated
                    segHovered -> CardElevated
                    segment == SegmentType.ADDON -> lerp(CardColor, accentColor, 0.10f)
                    else -> CardColor
                },
                animationSpec = tween(200)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(34.dp)
                    .clip(SegmentShape)
                    .hoverable(segInteraction)
                    .background(bgColor)
                    .border(1.dp, borderColor, SegmentShape)
                    .clickable { expanded = if (isExpanded) null else segment }
                    .padding(horizontal = 12.dp)
            ) {
                when (segment) {
                    SegmentType.ADDON -> AddonSegmentContent(currentAddon, addonId, accentColor)
                    SegmentType.SECTION -> SectionSegmentContent(isTactics, isLadder)
                    SegmentType.BRACKET -> BracketSegmentContent(currentGameMode, gameModeId)
                }
            }
        }

        // ─── Inline alternatives when a segment is expanded ─────
        if (expanded != null) {
            Spacer(Modifier.width(6.dp))

            when (expanded) {
                SegmentType.ADDON -> {
                    addons.filter { it.id != addonId }.forEach { addon ->
                        InlineAddonOption(
                            addon = addon,
                            enabled = addon.hasData,
                            onClick = {
                                expanded = null
                                homeViewModel.selectAddon(addon)
                                if (isTactics) homeViewModel.selectSection(HomeSection.TACTICS)
                                else if (isGuides) homeViewModel.selectSection(HomeSection.CLASS_GUIDES)
                                onNavigate(Screen.AddonSelection)
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                SegmentType.SECTION -> {
                    val alternatives = listOf(
                        "Tactics" to isTactics,
                        "Class Guides" to isGuides,
                        "Ladder" to isLadder
                    )
                    alternatives.filter { !it.second }.forEach { (label, _) ->
                        InlineOption(
                            label = label,
                            onClick = {
                                expanded = null
                                if (addonId != null) {
                                    when (label) {
                                        "Tactics" -> {
                                            val firstMode = gameModes.firstOrNull { it.hasData }
                                            if (firstMode != null) {
                                                onNavigate(Screen.CompositionSelection(addonId, firstMode.id))
                                            }
                                        }
                                        "Ladder" -> onNavigate(Screen.Ladder(addonId))
                                        else -> onNavigate(Screen.ClassGuideList(addonId))
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                SegmentType.BRACKET -> {
                    gameModes.filter { it.id != gameModeId }.forEach { mode ->
                        InlineOption(
                            label = "${mode.teamSize}v${mode.teamSize}",
                            enabled = mode.hasData,
                            onClick = {
                                expanded = null
                                if (addonId != null && mode.hasData) {
                                    onNavigate(Screen.CompositionSelection(addonId, mode.id))
                                }
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                else -> {}
            }
        }

        // ─── Trailing breadcrumbs (hidden when expanding) ───────
        if (expanded == null) {
            val trailingCrumbs = buildTrailingBreadcrumbs(currentScreen)
            if (trailingCrumbs.isNotEmpty()) {
                trailingCrumbs.forEach { crumb ->
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "›",
                        color = DividerColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    BreadcrumbChip(
                        label = crumb.label,
                        isCurrent = crumb.isCurrent,
                        onClick = crumb.target?.let { target -> { onNavigate(target) } }
                    )
                }
            }
        }
    }
}

// ─── Segment content composables ────────────────────────────────────────────

@Composable
private fun AddonSegmentContent(addon: Addon?, addonId: String?, accent: Color) {
    val cinzel = cinzelDecorative()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .border(1.5.dp, accent, CircleShape)
                .background(accent.copy(alpha = 0.15f), CircleShape)
        ) {
            Text(
                "W",
                fontFamily = cinzel,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accent,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = addon?.shortName ?: addonId?.formatId() ?: "",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SectionSegmentContent(isTactics: Boolean, isLadder: Boolean = false) {
    Text(
        text = when {
            isTactics -> "Tactics"
            isLadder  -> "Ladder"
            else      -> "Guides"
        },
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun BracketSegmentContent(mode: GameMode?, gameModeId: String?) {
    Text(
        text = mode?.let { "${it.teamSize}v${it.teamSize}" } ?: gameModeId?.formatId() ?: "",
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ─── Inline option composables (expand to the right) ────────────────────────

private val GreyedOut = Color(0xFF555555)

@Composable
private fun InlineAddonOption(addon: Addon, enabled: Boolean = true, onClick: () -> Unit) {
    val accent = if (enabled) parseHexColor(addon.accentColor) else GreyedOut
    val cinzel = cinzelDecorative()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .height(34.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(SegmentShape)
            .background(Surface)
            .border(1.dp, accent.copy(alpha = 0.4f), SegmentShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .border(1.dp, accent, CircleShape)
                .background(accent.copy(alpha = 0.15f), CircleShape)
        ) {
            Text("W", fontFamily = cinzel, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = accent)
        }
        Text(addon.shortName, color = if (enabled) TextSecondary else GreyedOut, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InlineOption(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(34.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(SegmentShape)
            .background(Surface)
            .border(1.dp, DividerColor, SegmentShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Trailing breadcrumbs ───────────────────────────────────────────────────

private data class TrailingCrumb(
    val label: String,
    val isCurrent: Boolean,
    val target: Screen?
)

private fun buildTrailingBreadcrumbs(screen: Screen): List<TrailingCrumb> = when (screen) {
    is Screen.CompositionSelection -> listOf(
        TrailingCrumb("Compositions", isCurrent = true, target = null)
    )
    is Screen.MatchupList -> listOf(
        TrailingCrumb(
            "Compositions",
            isCurrent = false,
            target = Screen.CompositionSelection(screen.addonId, screen.gameModeId)
        ),
        TrailingCrumb("Matchups", isCurrent = true, target = null)
    )
    is Screen.MatchupDetail -> listOf(
        TrailingCrumb(
            "Compositions",
            isCurrent = false,
            target = Screen.CompositionSelection(screen.addonId, screen.gameModeId)
        ),
        TrailingCrumb(
            "Matchups",
            isCurrent = false,
            target = Screen.MatchupList(screen.addonId, screen.gameModeId, screen.compositionId)
        ),
        TrailingCrumb("Detail", isCurrent = true, target = null)
    )
    is Screen.ClassGuideList -> listOf(
        TrailingCrumb("Classes", isCurrent = true, target = null)
    )
    is Screen.SpecGuide -> listOf(
        TrailingCrumb(
            "Classes",
            isCurrent = false,
            target = Screen.ClassGuideList(screen.addonId)
        ),
        TrailingCrumb(screen.specId.formatId(), isCurrent = true, target = null)
    )
    is Screen.Ladder -> listOf(
        TrailingCrumb("Ladder", isCurrent = true, target = null)
    )
    is Screen.PlayerDetail -> listOf(
        TrailingCrumb("Ladder", isCurrent = false, target = Screen.Ladder(screen.addonId)),
        TrailingCrumb("Player", isCurrent = true, target = null)
    )
    else -> emptyList()
}

@Composable
private fun BreadcrumbChip(
    label: String,
    isCurrent: Boolean,
    onClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(if (isCurrent) CardColor else Surface)
            .border(
                width = 0.5.dp,
                color = if (isCurrent) Primary else DividerColor,
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isCurrent) TextPrimary else TextSecondary,
            letterSpacing = 0.3.sp
        )
    }
}
