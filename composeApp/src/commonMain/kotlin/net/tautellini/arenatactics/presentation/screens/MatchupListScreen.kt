package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.MatchupListState
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.SpecBadge
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MatchupListScreen(
    addonId: String,
    gameModeId: String,
    compositionId: String,
    viewModel: MatchupListViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is MatchupListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        is MatchupListState.Error -> Text(
            s.message,
            color = TextSecondary,
            modifier = Modifier.padding(24.dp)
        )
        is MatchupListState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Background),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        s.matchups.forEach { matchup ->
                            Surface(
                                color = CardColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    onNavigate(Screen.MatchupDetail(addonId, gameModeId, compositionId, matchup.id))
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("vs", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                                    matchup.enemySpecIds.forEach { specId ->
                                        val spec  = s.specMap[specId]  ?: return@forEach
                                        val clazz = s.classMap[spec.classId] ?: return@forEach
                                        SpecBadge(spec, clazz)
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
