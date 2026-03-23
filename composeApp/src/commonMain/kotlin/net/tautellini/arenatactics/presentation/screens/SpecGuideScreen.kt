package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.presentation.SpecGuideState
import net.tautellini.arenatactics.presentation.SpecGuideViewModel
import net.tautellini.arenatactics.presentation.screens.components.SpecMetaContent
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun SpecGuideScreen(viewModel: SpecGuideViewModel) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SpecGuideState.Loading -> Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        is SpecGuideState.Error -> Box(Modifier.fillMaxSize().background(Background).padding(24.dp)) {
            Text(s.message, color = TextSecondary)
        }
        is SpecGuideState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SpecMetaContent(s.spec, s.wowClass, s.meta, s.items)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
