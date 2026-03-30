package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun TopBar(
    currentScreen: Screen,
    addonName: String?,
    bracketLabel: String?,
    isCompact: Boolean,
    onToggleDrawer: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateBack: (Screen) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(BgAbyss.copy(alpha = 0.8f))
            .padding(horizontal = if (isCompact) 14.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hamburger (mobile only)
        if (isCompact) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Menu",
                tint = TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onToggleDrawer)
            )
        }

        // Breadcrumbs
        BreadcrumbTrail(
            currentScreen = currentScreen,
            onNavigateHome = onNavigateHome,
            onNavigateBack = onNavigateBack
        )

        Spacer(Modifier.weight(1f))

        // Context badge
        if (addonName != null) {
            val contextText = if (bracketLabel != null) "$addonName · $bracketLabel" else addonName
            Text(
                contextText.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = TextDim,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    // Bottom border
    Box(
        Modifier.fillMaxWidth().height(1.dp).background(DividerColor)
    )
}

@Composable
private fun BreadcrumbTrail(
    currentScreen: Screen,
    onNavigateHome: () -> Unit,
    onNavigateBack: (Screen) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val crumbs = buildBreadcrumbs(currentScreen)
        crumbs.forEachIndexed { index, (label, target) ->
            if (index > 0) {
                Text("\u203A", fontSize = 10.sp, color = TextDim)
            }
            val isCurrent = index == crumbs.lastIndex
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                color = if (isCurrent) TextPrimary else TextMuted,
                modifier = if (!isCurrent && target != null) {
                    Modifier.clickable {
                        if (target is Screen.Dashboard) onNavigateHome() else onNavigateBack(target)
                    }
                } else Modifier
            )
        }
    }
}

private fun buildBreadcrumbs(screen: Screen): List<Pair<String, Screen?>> {
    return when (screen) {
        is Screen.Dashboard -> listOf("Dashboard" to null)
        is Screen.CompositionSelection -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Compositions" to null
        )
        is Screen.MatchupList -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Compositions" to Screen.CompositionSelection(screen.addonId, screen.gameModeId),
            "Matchups" to null
        )
        is Screen.MatchupDetail -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Matchups" to Screen.MatchupList(screen.addonId, screen.gameModeId, screen.compositionId),
            "Detail" to null
        )
        is Screen.Meta -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Meta" to null
        )
        is Screen.ClassGuideList -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Guides" to null
        )
        is Screen.SpecGuide -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Guides" to Screen.ClassGuideList(screen.addonId),
            screen.specId.replace("_", " ").replaceFirstChar { it.uppercase() } to null
        )
        is Screen.Ladder -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Ladder" to null
        )
        is Screen.PlayerDetail -> listOf(
            "Dashboard" to Screen.Dashboard,
            "Ladder" to Screen.Ladder(screen.addonId),
            "Player" to null
        )
    }
}
