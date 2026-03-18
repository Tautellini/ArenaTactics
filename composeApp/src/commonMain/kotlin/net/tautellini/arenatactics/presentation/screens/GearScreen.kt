package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.presentation.GearState
import net.tautellini.arenatactics.presentation.GearViewModel
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.ItemRow
import net.tautellini.arenatactics.presentation.theme.*
import net.tautellini.arenatactics.refreshWowheadTooltips

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

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton { navigator.pop() }
            Spacer(Modifier.width(12.dp))
            Text(
                text = compositionId.split("_").joinToString(" / ") { it.replaceFirstChar { c -> c.uppercase() } },
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
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

@Composable
private fun GearTabContent(viewModel: GearViewModel) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is GearState.Success) refreshWowheadTooltips()
    }

    when (val s = state) {
        is GearState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        is GearState.Error -> Text(
            s.message,
            color = TextSecondary,
            modifier = Modifier.padding(24.dp)
        )
        is GearState.Success -> {
            LazyColumn {
                s.gearByClass.forEach { (classId, phases) ->
                    val className = s.classNames[classId] ?: classId
                    phases.forEach { phase ->
                        item {
                            Text(
                                text = "$className — Phase ${phase.phase}",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
                            )
                        }
                        items(phase.items) { gearItem ->
                            ItemRow(gearItem)
                            HorizontalDivider(
                                color = DividerColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
