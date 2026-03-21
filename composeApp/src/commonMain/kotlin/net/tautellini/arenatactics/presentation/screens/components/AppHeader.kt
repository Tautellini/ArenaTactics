package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

// ─── Chevron shape ──────────────────────────────────────────────────────────

private val ARROW_SIZE = 12.dp

/**
 * Chevron/ribbon shape. All segments have an arrow point on the right.
 * Segments after the first also have a matching notch on the left so
 * they tile seamlessly in a Row.
 */
private class ChevronShape(
    private val arrowPx: Float,
    private val hasNotch: Boolean
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val a = arrowPx

        val path = Path().apply {
            if (hasNotch) {
                moveTo(0f, 0f)
                lineTo(w - a, 0f)
                lineTo(w, h / 2f)
                lineTo(w - a, h)
                lineTo(0f, h)
                lineTo(a, h / 2f)
            } else {
                moveTo(0f, 0f)
                lineTo(w - a, 0f)
                lineTo(w, h / 2f)
                lineTo(w - a, h)
                lineTo(0f, h)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// ─── Segment types ──────────────────────────────────────────────────────────

private enum class SegmentType { ADDON, SECTION, BRACKET }

// ─── Main composable ────────────────────────────────────────────────────────

@Composable
fun AppHeader(
    currentScreen: Screen,
    homeViewModel: HomeViewModel,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier
) {
    val homeState by homeViewModel.state.collectAsState()
    val addonId = currentScreen.extractAddonId()
    val gameModeId = currentScreen.extractGameModeId()
    val isTactics = currentScreen.isTacticsPath()
    val isGuides = currentScreen.isGuidesPath()

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

    // Close dropdown on screen change
    LaunchedEffect(currentScreen) { expanded = null }

    // Shield shimmer
    val transition = rememberInfiniteTransition(label = "shield-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer-x"
    )

    val arrowPx = with(LocalDensity.current) { ARROW_SIZE.toPx() }

    Column(modifier = Modifier.fillMaxWidth().background(Surface)) {

        // ─── Chevron bar ────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
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

            Spacer(Modifier.width(16.dp))

            // Determine which segments to show
            val segments = buildList {
                add(SegmentType.ADDON)
                add(SegmentType.SECTION)
                if (isTactics && gameModeId != null) add(SegmentType.BRACKET)
            }

            val accentColor = currentAddon?.let { parseHexColor(it.accentColor) } ?: Primary

            segments.forEachIndexed { index, segment ->
                val isFirst = index == 0
                val isExpanded = expanded == segment

                val bgColor = when {
                    isExpanded -> CardElevated
                    segment == SegmentType.ADDON -> lerp(CardColor, accentColor, 0.12f)
                    else -> CardColor
                }

                val shape = ChevronShape(arrowPx, hasNotch = !isFirst)

                val startPad = if (isFirst) 12.dp else ARROW_SIZE + 6.dp
                val endPad = ARROW_SIZE + 6.dp

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(shape)
                        .background(bgColor)
                        .clickable { expanded = if (isExpanded) null else segment }
                        .padding(start = startPad, end = endPad)
                ) {
                    when (segment) {
                        SegmentType.ADDON -> AddonSegmentContent(currentAddon, addonId, accentColor)
                        SegmentType.SECTION -> SectionSegmentContent(isTactics)
                        SegmentType.BRACKET -> BracketSegmentContent(currentGameMode, gameModeId)
                    }
                }
            }

            // ─── Trailing breadcrumbs for deeper screens ────────────
            val trailingCrumbs = buildTrailingBreadcrumbs(currentScreen)
            if (trailingCrumbs.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                trailingCrumbs.forEach { crumb ->
                    Text(
                        text = "›",
                        color = DividerColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    BreadcrumbChip(
                        label = crumb.label,
                        isCurrent = crumb.isCurrent,
                        onClick = crumb.target?.let { target -> { onNavigate(target) } }
                    )
                }
            }
        }

        // ─── Dropdown panels ────────────────────────────────────────

        // Addon dropdown
        AnimatedVisibility(
            visible = expanded == SegmentType.ADDON,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            DropdownPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    addons.filter { it.hasData }.forEach { addon ->
                        val isCurrent = addon.id == addonId
                        AddonDropdownOption(
                            addon = addon,
                            isCurrent = isCurrent,
                            onClick = {
                                expanded = null
                                if (!isCurrent) {
                                    homeViewModel.selectAddon(addon)
                                    if (isTactics) homeViewModel.selectSection(HomeSection.TACTICS)
                                    else if (isGuides) homeViewModel.selectSection(HomeSection.CLASS_GUIDES)
                                    onNavigate(Screen.AddonSelection)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section dropdown
        AnimatedVisibility(
            visible = expanded == SegmentType.SECTION,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            DropdownPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionDropdownOption(
                        label = "Tactics",
                        isCurrent = isTactics,
                        onClick = {
                            expanded = null
                            if (!isTactics && addonId != null) {
                                val firstMode = gameModes.firstOrNull { it.hasData }
                                if (firstMode != null) {
                                    onNavigate(Screen.CompositionSelection(addonId, firstMode.id))
                                }
                            }
                        }
                    )
                    SectionDropdownOption(
                        label = "Class Guides",
                        isCurrent = isGuides,
                        onClick = {
                            expanded = null
                            if (!isGuides && addonId != null) {
                                onNavigate(Screen.ClassGuideList(addonId))
                            }
                        }
                    )
                }
            }
        }

        // Bracket dropdown
        AnimatedVisibility(
            visible = expanded == SegmentType.BRACKET,
            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(150)) + fadeOut(tween(150))
        ) {
            DropdownPanel {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    gameModes.forEach { mode ->
                        BracketDropdownOption(
                            mode = mode,
                            isCurrent = mode.id == gameModeId,
                            onClick = {
                                expanded = null
                                if (mode.id != gameModeId && addonId != null && mode.hasData) {
                                    onNavigate(Screen.CompositionSelection(addonId, mode.id))
                                }
                            }
                        )
                    }
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
private fun SectionSegmentContent(isTactics: Boolean) {
    Text(
        text = if (isTactics) "Tactics" else "Guides",
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun BracketSegmentContent(mode: GameMode?, gameModeId: String?) {
    Text(
        text = mode?.description ?: gameModeId?.formatId() ?: "",
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ─── Dropdown panel wrapper ─────────────────────────────────────────────────

@Composable
private fun DropdownPanel(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardColor.copy(alpha = 0.97f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        content()
    }
}

// ─── Dropdown option composables ────────────────────────────────────────────

@Composable
private fun AddonDropdownOption(addon: Addon, isCurrent: Boolean, onClick: () -> Unit) {
    val accent = parseHexColor(addon.accentColor)
    val bg = if (isCurrent) CardElevated else Surface
    val borderColor = if (isCurrent) accent else DividerColor
    val cinzel = cinzelDecorative()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .border(1.5.dp, accent, CircleShape)
                .background(accent.copy(alpha = 0.15f), CircleShape)
        ) {
            Text("W", fontFamily = cinzel, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = accent)
        }
        Text(addon.shortName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionDropdownOption(label: String, isCurrent: Boolean, onClick: () -> Unit) {
    val bg = if (isCurrent) CardElevated else Surface
    val borderColor = if (isCurrent) Primary else DividerColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BracketDropdownOption(mode: GameMode, isCurrent: Boolean, onClick: () -> Unit) {
    val bg = if (isCurrent) CardElevated else Surface
    val borderColor = if (isCurrent) Primary else DividerColor
    val tileAlpha = if (mode.hasData) 1f else 0.35f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .alpha(tileAlpha)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(if (mode.hasData) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(mode.description, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Trailing breadcrumbs ───────────────────────────────────────────────────

private data class TrailingCrumb(
    val label: String,
    val isCurrent: Boolean,
    val target: Screen?
)

private fun buildTrailingBreadcrumbs(screen: Screen): List<TrailingCrumb> = when (screen) {
    is Screen.MatchupList -> listOf(
        TrailingCrumb("Matchups", isCurrent = true, target = null)
    )
    is Screen.MatchupDetail -> listOf(
        TrailingCrumb(
            "Matchups",
            isCurrent = false,
            target = Screen.MatchupList(screen.addonId, screen.gameModeId, screen.compositionId)
        ),
        TrailingCrumb("Detail", isCurrent = true, target = null)
    )
    is Screen.SpecGuide -> listOf(
        TrailingCrumb(screen.specId.formatId(), isCurrent = true, target = null)
    )
    else -> emptyList()
}

@Composable
private fun BreadcrumbChip(
    label: String,
    isCurrent: Boolean,
    onClick: (() -> Unit)?
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .background(if (isCurrent) CardColor else Surface)
            .border(
                width = 0.5.dp,
                color = if (isCurrent) Primary else DividerColor,
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
