package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.navigation.Navigator
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.CompositionSelectionState
import net.tautellini.arenatactics.presentation.CompositionSelectionViewModel
import net.tautellini.arenatactics.presentation.screens.components.BackButton
import net.tautellini.arenatactics.presentation.screens.components.CompositionCard
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun CompositionSelectionScreen(
    gameModeId: String,
    viewModel: CompositionSelectionViewModel,
    navigator: Navigator
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton { navigator.pop() }
            Spacer(Modifier.width(12.dp))
            Text(
                "Select Composition",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(24.dp))
        when (val s = state) {
            is CompositionSelectionState.Loading -> CircularProgressIndicator(color = Accent)
            is CompositionSelectionState.Error -> Text(s.message, color = TextSecondary)
            is CompositionSelectionState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.compositions) { rich ->
                        CompositionCard(rich) {
                            navigator.push(Screen.GearView(gameModeId, rich.composition.id))
                        }
                    }
                }
            }
        }
    }
}
