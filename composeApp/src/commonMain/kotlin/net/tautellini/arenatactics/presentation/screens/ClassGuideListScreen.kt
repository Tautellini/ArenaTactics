package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.ClassGuideListState
import net.tautellini.arenatactics.presentation.ClassGuideListViewModel
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun ClassGuideListScreen(
    addonId: String,
    viewModel: ClassGuideListViewModel,
    onNavigate: (Screen) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(24.dp)) {
        Text(
            "Class Guides",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        when (val s = state) {
            is ClassGuideListState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            is ClassGuideListState.Error   -> Text(s.message, color = TextSecondary)
            is ClassGuideListState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.specs) { spec ->
                        val wowClass = s.classMap[spec.classId]
                        SpecGuideCard(spec = spec, wowClass = wowClass) {
                            if (spec.hasData) {
                                onNavigate(Screen.SpecGuide(addonId, spec.classId, spec.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecGuideCard(spec: WowSpec, wowClass: WowClass?, onClick: () -> Unit) {
    val enabled = spec.hasData
    val cardAlpha = if (enabled) 1f else 0.45f
    val grayscaleFilter = if (enabled) null else ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) }
    )

    Surface(
        color = CardColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .alpha(cardAlpha)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = WowheadIcons.large(spec.iconName),
                contentDescription = spec.name,
                colorFilter = grayscaleFilter,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
            )
            Text(spec.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (wowClass != null) {
                Text(wowClass.name, color = TextSecondary, fontSize = 11.sp)
            }
            if (!enabled) {
                Text("Coming Soon", color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}
