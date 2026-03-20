package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.data.model.GearItem
import net.tautellini.arenatactics.data.model.GearPhase
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.openUrl
import net.tautellini.arenatactics.presentation.GearState
import net.tautellini.arenatactics.presentation.GearViewModel
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.GearIcon
import net.tautellini.arenatactics.presentation.screens.components.SpecBadge
import net.tautellini.arenatactics.presentation.theme.*

enum class CompositionTab { GEAR, MATCHUPS }

@Composable
fun CompositionHubScreen(
    gameModeId: String,
    compositionId: String,
    gearViewModel: GearViewModel,
    matchupListViewModel: MatchupListViewModel,
    navigator: Navigator
) {
    var selectedTab by remember { mutableStateOf(CompositionTab.GEAR) }
    val gearState by gearViewModel.state.collectAsState()
    val richComposition = (gearState as? GearState.Success)?.richComposition

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackButton { navigator.pop() }
            if (richComposition != null) {
                richComposition.specs.zip(richComposition.classes).forEach { (spec, wowClass) ->
                    SpecBadge(spec, wowClass)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            CompositionTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (selected) Accent else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        HorizontalDivider(color = DividerColor)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                CompositionTab.GEAR -> GearTabContent(gearViewModel)
                CompositionTab.MATCHUPS -> MatchupListScreen(
                    gameModeId = gameModeId,
                    compositionId = compositionId,
                    viewModel = matchupListViewModel,
                    navigator = navigator
                )
            }
        }
    }
}

// ─── Slot ordering for paper doll layout ────────────────────────────────────

private val LEFT_SLOTS   = listOf("Head", "Neck", "Shoulders", "Back", "Chest", "Wrists")
private val RIGHT_SLOTS  = listOf("Hands", "Waist", "Legs", "Feet", "Ring", "Ring")
private val BOTTOM_SLOTS = listOf("Trinket", "Trinket", "Main Hand", "Off Hand", "Ranged")

/** "Wand" (Mage) occupies the same bottom-row position as "Ranged" (Rogue). */
private fun normalizeSlot(s: String) = if (s == "Wand") "Ranged" else s

/**
 * Maps a flat item list to an ordered slot list.
 * Handles duplicate slots (Ring×2, Trinket×2) by consuming items greedily.
 * Returns null for slots with no matching item.
 */
private fun mapItemsToSlots(items: List<GearItem>, slotList: List<String>): List<GearItem?> {
    val remaining = items.toMutableList()
    return slotList.map { slot ->
        val idx = remaining.indexOfFirst { normalizeSlot(it.slot) == slot }
        if (idx >= 0) remaining.removeAt(idx) else null
    }
}

// ─── GearTabContent (replaces the old flat-list version) ────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GearTabContent(viewModel: GearViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is GearState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        is GearState.Error -> Text(s.message, color = TextSecondary, modifier = Modifier.padding(24.dp))
        is GearState.Success -> {
            var selectedPhase by remember { mutableStateOf(1) }

            val availablePhases = remember(s) {
                s.gearByClass.values.firstOrNull()?.map { it.phase }?.sorted() ?: listOf(1)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Phase tabs
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                    availablePhases.forEach { phase ->
                        val selected = phase == selectedPhase
                        Box(
                            modifier = Modifier
                                .clickable { selectedPhase = phase }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Phase $phase",
                                color = if (selected) Accent else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                HorizontalDivider(color = DividerColor)

                // Two paper dolls in a flow row (side-by-side on wide screens, stacked on narrow)
                FlowRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    s.gearByClass.forEach { (classId, phases) ->
                        val className = s.classNames[classId] ?: classId
                        val phase = phases.find { it.phase == selectedPhase } ?: phases.firstOrNull()
                        if (phase != null) {
                            PaperDoll(
                                classId = classId,
                                className = className,
                                phase = phase,
                                modifier = Modifier.widthIn(min = 280.dp, max = 420.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── PaperDoll ───────────────────────────────────────────────────────────────

@Composable
private fun PaperDoll(
    classId: String,
    className: String,
    phase: GearPhase,
    modifier: Modifier = Modifier
) {
    val leftItems   = remember(phase) { mapItemsToSlots(phase.items, LEFT_SLOTS) }
    val rightItems  = remember(phase) { mapItemsToSlots(phase.items, RIGHT_SLOTS) }
    val bottomItems = remember(phase) { mapItemsToSlots(phase.items, BOTTOM_SLOTS) }

    Surface(
        color = CardColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.padding(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top: left slots | class icon center | right slots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    leftItems.zip(LEFT_SLOTS).forEach { (item, slot) ->
                        if (item != null) GearSlot(item) else EmptyGearSlot(slot)
                    }
                }

                // Center: class icon + name
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GearIcon(
                        url = "https://wow.zamimg.com/images/wow/icons/large/classicon_$classId.jpg",
                        contentDescription = className,
                        modifier = Modifier.size(72.dp),
                        cornerRadius = 8.dp,
                        borderColor = classColor(classId),
                        borderWidth = 2.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = className,
                        color = classColor(classId),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Right column
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rightItems.zip(RIGHT_SLOTS).forEach { (item, slot) ->
                        if (item != null) GearSlot(item) else EmptyGearSlot(slot)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(8.dp))

            // Bottom row: trinkets + weapons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                bottomItems.zip(BOTTOM_SLOTS).forEach { (item, slot) ->
                    if (item != null) GearSlot(item, modifier = Modifier.weight(1f))
                    else EmptyGearSlot(slot, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── GearSlot ────────────────────────────────────────────────────────────────

@Composable
private fun GearSlot(item: GearItem, modifier: Modifier = Modifier) {
    val wowheadUrl = "https://www.wowhead.com/tbc/item=${item.wowheadId}"
    Column(
        modifier = modifier
            .widthIn(min = 60.dp, max = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable { openUrl(wowheadUrl) }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GearIcon(
            url = "https://wow.zamimg.com/images/wow/icons/medium/${item.icon}.jpg",
            contentDescription = item.name,
            modifier = Modifier.size(48.dp),
            cornerRadius = 6.dp,
            wowheadItemId = item.wowheadId
        )
        Text(
            text = item.name,
            color = Accent,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
        if (item.enchant != null) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(10.dp), tint = Accent)
        }
    }
}

// ─── EmptyGearSlot ───────────────────────────────────────────────────────────

@Composable
private fun EmptyGearSlot(slotName: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(min = 60.dp, max = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardElevated)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(24.dp), tint = TextSecondary)
        }
        Text(
            text = slotName,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

