package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import tautellini.arenatactics.generated.resources.Res
import tautellini.arenatactics.generated.resources.logo_at
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.NavSection
import net.tautellini.arenatactics.presentation.NavigationState
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun Sidebar(
    state: NavigationState,
    onSelectAddon: (String) -> Unit,
    onSelectSection: (NavSection) -> Unit,
    onSelectBracket: (String) -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Brand ──
        SidebarBrand(onClick = onGoHome)

        // ── Expansion ──
        SectionLabel("Expansion")
        state.addons.forEach { addon ->
            val isActive = state.selectedAddonId == addon.id
            val accentColor = try {
                val hex = addon.accentColor.removePrefix("#")
                Color(("FF$hex").toLong(16))
            } catch (_: Throwable) { Primary }
            AddonItem(
                name = addon.name,
                accentColor = accentColor,
                isActive = isActive,
                isDisabled = !addon.hasData,
                onClick = { onSelectAddon(addon.id) }
            )
        }

        SidebarDivider()

        // ── Sections (visible when addon selected) ──
        if (state.selectedAddonId != null) {
            SectionLabel("Sections")
            val selectedAddon = state.addons.find { it.id == state.selectedAddonId }
            val hasTactics = state.gameModes.any { it.hasData }
            val hasMeta = selectedAddon?.hasData == true
            val hasLadder = state.selectedAddonId in state.addonsWithLadder

            NavLink(
                icon = Icons.Rounded.Shield,
                label = "Tactics",
                isActive = state.selectedSection == NavSection.TACTICS,
                isDisabled = !hasTactics,
                onClick = { onSelectSection(NavSection.TACTICS) }
            )
            NavLink(
                icon = Icons.Rounded.Stars,
                label = "Meta",
                isActive = state.selectedSection == NavSection.META,
                isDisabled = !hasMeta,
                onClick = { onSelectSection(NavSection.META) }
            )
            NavLink(
                icon = Icons.Rounded.Leaderboard,
                label = "Ladder",
                isActive = state.selectedSection == NavSection.LADDER,
                isDisabled = !hasLadder,
                onClick = { onSelectSection(NavSection.LADDER) }
            )

            // ── Bracket (visible when TACTICS selected) ──
            if (state.selectedSection == NavSection.TACTICS && state.gameModes.isNotEmpty()) {
                SidebarDivider()
                SectionLabel("Bracket")
                state.gameModes.forEach { mode ->
                    NavLink(
                        textIcon = "${mode.teamSize}v${mode.teamSize}",
                        label = "Arena ${mode.teamSize}v${mode.teamSize}",
                        isActive = state.selectedGameModeId == mode.id,
                        isDisabled = !mode.hasData,
                        onClick = { onSelectBracket(mode.id) }
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Footer ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Made with love for Kizaru",
                fontSize = 10.sp,
                color = TextDim,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun SidebarBrand(onClick: () -> Unit) {
    val cinzelFont = cinzel()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                Brush.verticalGradient(
                    listOf(BgStone, BgDeep)
                )
            )
    ) {
        // Atmospheric glow overlay
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Primary.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(130f, 50f),
                        radius = 220f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 24.dp, 20.dp, 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AT monogram logo
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.logo_at),
                contentDescription = "Arena Tactics",
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "ARENA",
                fontFamily = cinzelFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 8.sp,
                color = TextHero
            )
            Text(
                "TACTICS",
                fontFamily = cinzelFont,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                letterSpacing = 12.sp,
                color = Primary
            )
            Spacer(Modifier.height(8.dp))
            // Decorative line
            Box(
                Modifier
                    .width(80.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Primary.copy(alpha = 0.4f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "WORLD OF WARCRAFT PVP",
                fontSize = 8.sp,
                letterSpacing = 3.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
    // Bottom border with glow
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Primary.copy(alpha = 0.2f), Color.Transparent)
                )
            )
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 3.sp,
        color = TextDim,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun AddonItem(
    name: String,
    accentColor: Color,
    isActive: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        when {
            isActive -> PrimaryDim
            isHovered -> Color.White.copy(alpha = 0.03f)
            else -> Color.Transparent
        }, tween(150)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .hoverable(interactionSource)
                .clickable(enabled = !isDisabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .then(if (isDisabled) Modifier.fillMaxWidth() else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active indicator bar
            if (isActive) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(0.dp, 2.dp, 2.dp, 0.dp))
                        .background(accentColor)
                )
            }

            // Accent dot
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDisabled) accentColor.copy(alpha = 0.25f) else accentColor)
            )

            Text(
                name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when {
                    isDisabled -> TextDim
                    isActive -> TextHero
                    else -> TextSecondary
                }
            )
        }
    }
}

@Composable
private fun NavLink(
    icon: ImageVector? = null,
    textIcon: String? = null,
    label: String,
    isActive: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        when {
            isActive -> PrimaryDim
            isHovered -> Color.White.copy(alpha = 0.03f)
            else -> Color.Transparent
        }, tween(150)
    )

    val iconColor = when {
        isDisabled -> TextDim
        isActive -> Primary
        else -> TextMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .hoverable(interactionSource)
            .clickable(enabled = !isDisabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active indicator
        if (isActive) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(0.dp, 2.dp, 2.dp, 0.dp))
                    .background(Primary)
            )
        }

        // Icon (either Material icon or text)
        Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            } else if (textIcon != null) {
                Text(
                    textIcon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }
        }

        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                isDisabled -> TextDim
                isActive -> TextHero
                else -> TextSecondary
            }
        )
    }
}

@Composable
private fun SidebarDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(1.dp)
            .background(DividerColor)
    )
}
