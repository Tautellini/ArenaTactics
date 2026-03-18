package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.presentation.MatchupDetailState
import net.tautellini.arenatactics.presentation.MatchupDetailViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.ClassBadge
import net.tautellini.arenatactics.presentation.screens.components.MarkdownText
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MatchupDetailScreen(
    viewModel: MatchupDetailViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        when (val s = state) {
            is MatchupDetailState.Loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
            is MatchupDetailState.Error -> Text(
                s.message,
                color = TextSecondary,
                modifier = Modifier.padding(24.dp)
            )
            is MatchupDetailState.Success -> {
                val matchup = s.matchup
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton { navigator.pop() }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "vs",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    val c1 = s.classMap[matchup.enemyClass1Id]
                    val c2 = s.classMap[matchup.enemyClass2Id]
                    if (c1 != null) ClassBadge(c1.id, c1.name, modifier = Modifier.padding(end = 6.dp))
                    if (c2 != null) ClassBadge(c2.id, c2.name)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    MarkdownText(matchup.strategyMarkdown)
                }
            }
        }
    }
}
