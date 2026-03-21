package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.theme.Primary
import net.tautellini.arenatactics.presentation.theme.CardColor
import net.tautellini.arenatactics.presentation.theme.DividerColor
import net.tautellini.arenatactics.presentation.theme.Surface
import net.tautellini.arenatactics.presentation.theme.TextPrimary
import net.tautellini.arenatactics.presentation.theme.TextSecondary

/**
 * Persistent header shown on all non-home screens.
 *
 * The shield is rendered via [shieldModifier] so the caller can inject
 * the `sharedElement()` modifier for the home→header shared transition.
 * Breadcrumb chips are derived from [Screen.buildStack] of [currentScreen].
 * Ancestor chips are clickable and invoke [onNavigate].
 */
@Composable
fun AppHeader(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    shieldModifier: Modifier = Modifier
) {
    val stack = Screen.buildStack(currentScreen)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Small shield — caller injects sharedElement() + size() via shieldModifier.
        // Clicking the shield navigates home.
        Box(modifier = Modifier.clickable { onNavigate(Screen.AddonSelection) }) {
            ShieldCanvas(modifier = shieldModifier)
        }

        // Breadcrumb chips — skip first (AddonSelection = shield)
        stack.drop(1).forEachIndexed { index, screen ->
            val isCurrent = screen == currentScreen
            val isAncestor = !isCurrent

            // Chevron separator
            Text(
                text = "›",
                color = DividerColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )

            BreadcrumbChip(
                label = screen.breadcrumbLabel(),
                isCurrent = isCurrent,
                onClick = if (isAncestor) ({ onNavigate(screen) }) else null
            )
        }
    }
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

private fun Screen.breadcrumbLabel(): String = when (this) {
    is Screen.AddonSelection    -> "Home"
    is Screen.AddonHub          -> addonId.formatId()
    is Screen.GameModeSelection -> "Tactics"
    is Screen.CompositionSelection -> gameModeId.formatId()
    is Screen.MatchupList       -> "Matchups"
    is Screen.MatchupDetail     -> "Detail"
    is Screen.ClassGuideList    -> "Class Guides"
    is Screen.SpecGuide         -> specId.formatId()
}

/** Converts "tbc-2v2" → "TBC 2v2", "some_id" → "Some Id". */
private fun String.formatId(): String =
    replace('-', ' ').replace('_', ' ')
        .split(' ')
        .joinToString(" ") { word ->
            if (word.all { it.isDigit() || it == 'v' }) word.uppercase()
            else word.replaceFirstChar { it.uppercaseChar() }
        }
