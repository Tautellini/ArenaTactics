package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.theme.classColor

@Composable
fun SpecBadge(
    spec: WowSpec,
    wowClass: WowClass,
    modifier: Modifier = Modifier
) {
    val color = classColor(wowClass.id)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = WowheadIcons.large(spec.iconName),
            contentDescription = spec.name,
            modifier = Modifier.size(64.dp)
        )
    }
}
