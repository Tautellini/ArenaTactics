package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.MatchupListState
import net.tautellini.arenatactics.presentation.MatchupListViewModel
import net.tautellini.arenatactics.presentation.screens.components.ClassBadge
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MatchupListScreen(
    gameModeId: String,
    compositionId: String,
    viewModel: MatchupListViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is MatchupListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
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
                items(s.matchups) { matchup ->
                    Surface(
                        color = CardColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            navigator.push(
                                Screen.MatchupDetail(gameModeId, compositionId, matchup.id)
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "vs",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(20.dp)
                            )
                            val c1 = s.classMap[matchup.enemyClass1Id]
                            val c2 = s.classMap[matchup.enemyClass2Id]
                            if (c1 != null) ClassBadge(c1.id, c1.name)
                            if (c2 != null) ClassBadge(c2.id, c2.name)
                        }
                    }
                }
            }
        }
    }
}
