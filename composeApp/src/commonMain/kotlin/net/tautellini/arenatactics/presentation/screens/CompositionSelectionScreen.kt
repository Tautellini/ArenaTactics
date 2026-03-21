package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.CompositionTier
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.CompositionSelectionState
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.ClassFilterBar
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*

private fun CompositionTier.label() = when (this) {
    CompositionTier.DOMINANT -> "Dominant"
    CompositionTier.STRONG   -> "Strong"
    CompositionTier.PLAYABLE -> "Playable"
    CompositionTier.OTHERS   -> "Others"
}

@Composable
fun CompositionSelectionScreen(
    addonId: String,
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var othersExpanded by remember { mutableStateOf(false) }
    var selectedClassId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        when (val s = state) {
            is CompositionSelectionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            is CompositionSelectionState.Error ->
                Text(s.message, color = TextSecondary)
            is CompositionSelectionState.Success -> {
                ClassFilterBar(
                    classes = s.classes,
                    selectedClassId = selectedClassId,
                    onSelect = { selectedClassId = it },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompositionTier.entries.forEach { tier ->
                        val allComps = s.grouped[tier] ?: return@forEach
                        val comps = if (selectedClassId != null) {
                            allComps.filter { rc ->
                                rc.classes.any { it.id == selectedClassId }
                            }
                        } else allComps
                        if (comps.isEmpty()) return@forEach
                        if (tier == CompositionTier.OTHERS) {
                            item {
                                TierHeader(
                                    label = tier.label(),
                                    expandable = true,
                                    expanded = othersExpanded,
                                    onToggle = { othersExpanded = !othersExpanded }
                                )
                            }
                            if (othersExpanded) {
                                item {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        comps.forEach { rich ->
                                            CompositionCard(
                                                richComposition = rich,
                                                onClick = if (rich.composition.hasData) {
                                                    { onNavigate(Screen.MatchupList(addonId, gameModeId, rich.composition.id)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            item { TierHeader(label = tier.label()) }
                            item {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    comps.forEach { rich ->
                                        CompositionCard(
                                            richComposition = rich,
                                            onClick = if (rich.composition.hasData) {
                                                { onNavigate(Screen.MatchupList(addonId, gameModeId, rich.composition.id)) }
                                            } else null
                                        )
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

@Composable
private fun TierHeader(
    label: String,
    expandable: Boolean = false,
    expanded: Boolean = false,
    onToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (expandable) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (expandable) {
            Icon(
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextSecondary
            )
        }
    }
}
