package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MenuBook
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.AddonHubState
import net.tautellini.arenatactics.presentation.AddonHubViewModel
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun AddonHubScreen(
    addonId: String,
    viewModel: AddonHubViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is AddonHubState.Loading -> CircularProgressIndicator(color = Primary)
            is AddonHubState.Error   -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
            is AddonHubState.Success -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        s.addon.name,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
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
                        SectionTile(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Tactics",
                            subtitle = "Compositions & matchup guides",
                            onClick = { onNavigate(Screen.GameModeSelection(addonId)) }
                        )
                        SectionTile(
                            icon = Icons.Rounded.MenuBook,
                            title = "Class Guides",
                            subtitle = "Best-in-slot gear per spec",
                            onClick = { onNavigate(Screen.ClassGuideList(addonId)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(min = 160.dp, max = 280.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
