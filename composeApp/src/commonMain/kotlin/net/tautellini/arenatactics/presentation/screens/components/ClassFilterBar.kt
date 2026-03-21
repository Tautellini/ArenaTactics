package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.theme.CardColor
import net.tautellini.arenatactics.presentation.theme.CardElevated
import net.tautellini.arenatactics.presentation.theme.DividerColor
import net.tautellini.arenatactics.presentation.theme.TextSecondary
import net.tautellini.arenatactics.presentation.theme.classColor

@Composable
fun ClassFilterBar(
    classes: List<WowClass>,
    selectedClassId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        classes.forEach { wowClass ->
            val isSelected = selectedClassId == wowClass.id
            ClassChip(
                wowClass = wowClass,
                isSelected = isSelected,
                onClick = { onSelect(if (isSelected) null else wowClass.id) }
            )
        }
    }
}

@Composable
private fun ClassChip(
    wowClass: WowClass,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = classColor(wowClass.id)
    val shape = RoundedCornerShape(10.dp)
    val bg = if (isSelected) CardElevated else CardColor
    val borderColor = if (isSelected) color else DividerColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .alpha(if (isSelected) 1f else 0.6f)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .padding(2.dp)
        ) {
            AsyncImage(
                model = WowheadIcons.large(wowClass.iconName),
                contentDescription = wowClass.name,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = wowClass.name,
            color = if (isSelected) color else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
