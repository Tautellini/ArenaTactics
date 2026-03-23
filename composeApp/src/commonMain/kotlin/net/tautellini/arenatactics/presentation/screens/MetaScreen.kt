package net.tautellini.arenatactics.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.MetaState
import net.tautellini.arenatactics.presentation.MetaViewModel
import net.tautellini.arenatactics.presentation.SpecMetaState
import net.tautellini.arenatactics.presentation.screens.components.ClassFilterBar
import net.tautellini.arenatactics.presentation.screens.components.SPEC_ICON_NAMES
import net.tautellini.arenatactics.presentation.screens.components.SpecMetaContent
import net.tautellini.arenatactics.presentation.theme.*

@Composable
fun MetaScreen(viewModel: MetaViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val selectedSpecId by viewModel.selectedSpecId.collectAsState()
    val specMetaState by viewModel.specMetaState.collectAsState()

    when (val s = state) {
        is MetaState.Loading -> Box(
            Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        is MetaState.Error -> Box(Modifier.fillMaxSize().background(Background).padding(24.dp)) {
            Text(s.message, color = TextSecondary)
        }
        is MetaState.Success -> MetaContent(
            state = s,
            selectedClassId = selectedClassId,
            selectedSpecId = selectedSpecId,
            specMetaState = specMetaState,
            onSelectClass = viewModel::selectClass,
            onSelectSpec = viewModel::selectSpec
        )
    }
}

@Composable
private fun MetaContent(
    state: MetaState.Success,
    selectedClassId: String?,
    selectedSpecId: String?,
    specMetaState: SpecMetaState,
    onSelectClass: (String?) -> Unit,
    onSelectSpec: (String?) -> Unit
) {
    val classes = state.classMap.values.toList().sortedBy { it.name }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Class filter bar
        ClassFilterBar(
            classes = classes,
            selectedClassId = selectedClassId,
            onSelect = onSelectClass
        )

        // Spec filter row (visible when a class is selected)
        if (selectedClassId != null) {
            val specsForClass = state.specs.filter { it.classId == selectedClassId }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                specsForClass.forEach { spec ->
                    val hasData = spec.id in state.specsWithData
                    val isSelected = selectedSpecId == spec.id
                    SpecChip(
                        spec = spec,
                        isSelected = isSelected,
                        hasData = hasData,
                        classColor = classColor(selectedClassId),
                        onClick = { if (hasData) onSelectSpec(spec.id) }
                    )
                }
            }
        }

        // Spec meta content
        when (specMetaState) {
            is SpecMetaState.Idle -> {
                if (selectedClassId == null) {
                    Text(
                        "Select a class to view meta data",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                } else if (selectedSpecId == null) {
                    Text(
                        "Select a specialization above",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            is SpecMetaState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is SpecMetaState.Ready -> {
                SpecMetaContent(
                    spec = specMetaState.spec,
                    wowClass = specMetaState.wowClass,
                    meta = specMetaState.meta,
                    items = state.allItems,
                    talentTree = specMetaState.talentTree
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SpecChip(
    spec: WowSpec,
    isSelected: Boolean,
    hasData: Boolean,
    classColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg by animateColorAsState(
        when {
            isSelected -> CardElevated
            isHovered && hasData -> CardElevated
            else -> CardColor
        },
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        when {
            isSelected -> classColor
            isHovered && hasData -> classColor.copy(alpha = 0.4f)
            else -> CardBorder
        },
        animationSpec = tween(200)
    )
    val chipAlpha = when {
        !hasData -> 0.4f
        isSelected -> 1f
        isHovered -> 0.85f
        else -> 0.6f
    }
    val grayscaleFilter = if (hasData) null else ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) }
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(shape)
            .hoverable(interactionSource)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .then(if (hasData) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(chipAlpha)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val iconName = SPEC_ICON_NAMES[spec.id] ?: spec.iconName
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (hasData) classColor else CardBorder)
                .padding(2.dp)
        ) {
            AsyncImage(
                model = WowheadIcons.large(iconName),
                contentDescription = spec.name,
                colorFilter = grayscaleFilter,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = spec.name,
            color = if ((isSelected || isHovered) && hasData) classColor else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (!hasData) {
            Text(
                text = "No Data",
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
