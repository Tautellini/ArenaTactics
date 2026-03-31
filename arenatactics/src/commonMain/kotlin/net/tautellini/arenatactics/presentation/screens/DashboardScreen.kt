package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.auth.AuthState
import net.tautellini.arenatactics.auth.OAuthProvider
import net.tautellini.arenatactics.presentation.DashboardViewModel
import net.tautellini.arenatactics.presentation.theme.*
import net.tautellini.models.arenatactics.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    selectedAddonId: String?,
    authState: AuthState,
    onSignIn: (OAuthProvider) -> Unit
) {
    val data by viewModel.data.collectAsState()

    LaunchedEffect(selectedAddonId) {
        if (selectedAddonId != null) {
            viewModel.loadForAddon(selectedAddonId)
        }
    }

    if (selectedAddonId == null || data.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Greeting or Personalization CTA ──
        if (authState is AuthState.Authenticated) {
            item {
                val firstName = authState.name?.split(" ")?.firstOrNull() ?: "Champion"
                val cinzelFont = cinzel()
                Text(
                    "Hello, $firstName",
                    fontFamily = cinzelFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp,
                    color = TextHero,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        } else {
            item {
                PersonalizationCard(onSignIn = onSignIn)
            }
        }

        // ── Default data cards ──
        item {
            DashCard(title = "LADDER OVERVIEW", badge = "All Brackets") {
                val snapshots = data.ladderSnapshots
                if (snapshots.isEmpty()) {
                    Text("No ladder data available", fontSize = 12.sp, color = TextMuted)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        snapshots.entries.take(3).forEach { (key, snapshot) ->
                            StatBox(
                                value = snapshot.topEntries.firstOrNull()?.rating?.toString() ?: "—",
                                label = "Peak $key",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            DashCard(title = "TOP COMPOSITIONS", badge = "All Brackets") {
                if (data.topCompositions.isEmpty()) {
                    Text("No composition data", fontSize = 12.sp, color = TextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.topCompositions.forEach { rich ->
                            FeaturedCompRow(rich)
                        }
                    }
                }
            }
        }

        item {
            DashCard(title = "CLASS DISTRIBUTION", badge = "All Brackets") {
                if (data.classDistribution.isEmpty()) {
                    Text("No data", fontSize = 12.sp, color = TextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        data.classDistribution.forEach { entry ->
                            MiniDistRow(entry)
                        }
                    }
                }
            }
        }

        item {
            DashCard(title = "TOP PLAYERS", badge = "All Brackets") {
                if (data.topPlayers.isEmpty()) {
                    Text("No player data", fontSize = 12.sp, color = TextMuted)
                } else {
                    Column {
                        data.topPlayers.forEachIndexed { index, entry ->
                            MiniPlayerRow(rank = index + 1, entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalizationCard(onSignIn: (OAuthProvider) -> Unit) {
    val cinzelFont = cinzel()
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(CardColor)
            .border(1.dp, Primary.copy(alpha = 0.15f), shape)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Rounded.Dashboard,
            contentDescription = null,
            tint = Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp)
        )
        Text(
            "Your Personal Dashboard",
            fontFamily = cinzelFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            color = TextHero,
            textAlign = TextAlign.Center
        )
        Text(
            "Sign in to customize this dashboard with cards from any expansion, bracket, or spec. Pin gear guides, ladder stats, composition tiers — all in one place.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Primary.copy(alpha = 0.12f))
                .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .clickable { onSignIn(OAuthProvider.GOOGLE) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.PersonAdd,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Sign in with Google",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        }
    }
}

@Composable
internal fun DashCard(
    title: String,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cinzelFont = cinzel()
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(CardColor)
            .border(1.dp, CardBorder, shape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp, 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontFamily = cinzelFont,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = TextSecondary
            )
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryDim)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CardBorder))
        Column(
            modifier = Modifier.padding(14.dp, 12.dp),
            content = content
        )
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BgAbyss.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(Modifier.height(2.dp))
        Text(label.uppercase(), fontSize = 9.sp, letterSpacing = 1.sp, color = TextMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FeaturedCompRow(rich: RichComposition) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgAbyss.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            rich.classes.forEachIndexed { i, wowClass ->
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(classColor(wowClass.id)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        wowClass.name.take(1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BgVoid.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Text(
            rich.specs.joinToString(" / ") { it.name },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        val tierLabel = when (rich.composition.tier) {
            CompositionTier.DOMINANT -> "S+"
            CompositionTier.STRONG -> "A"
            CompositionTier.PLAYABLE -> "B"
            CompositionTier.OTHERS -> "C"
        }
        val tierColor = when (rich.composition.tier) {
            CompositionTier.DOMINANT -> GoldBright
            CompositionTier.STRONG -> Primary
            else -> TextMuted
        }
        val tierBg = when (rich.composition.tier) {
            CompositionTier.DOMINANT -> GoldDim
            CompositionTier.STRONG -> PrimaryDim
            else -> BgStone
        }
        Text(
            tierLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = tierColor,
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(tierBg)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MiniDistRow(entry: ClassDistributionEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(classColor(entry.classId))
        )
        Text(
            entry.classId.replaceFirstChar { it.uppercase() },
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.width(55.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BgAbyss)
        ) {
            val maxPct = 20.0
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((entry.percentage / maxPct).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(classColor(entry.classId).copy(alpha = 0.7f))
            )
        }
        Text(
            "${entry.percentage}%",
            fontSize = 10.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun MiniPlayerRow(rank: Int, entry: LadderEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Text(
            rank.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) Gold else TextDim,
            modifier = Modifier.width(24.dp)
        )
        val classId = entry.classId ?: "warrior"
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(classColor(classId)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                classId.take(1).uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = BgVoid.copy(alpha = 0.7f)
            )
        }
        Text(
            entry.characterName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = classColor(classId),
            modifier = Modifier.weight(1f)
        )
        Text(
            entry.rating.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
    }
}
