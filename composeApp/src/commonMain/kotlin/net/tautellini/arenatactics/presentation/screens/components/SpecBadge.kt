package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.tautellini.arenatactics.data.model.WowClass
import net.tautellini.arenatactics.data.model.WowSpec
import net.tautellini.arenatactics.data.model.WowheadIcons
import net.tautellini.arenatactics.presentation.theme.Background
import net.tautellini.arenatactics.presentation.theme.classColor

@Composable
fun SpecBadge(
    spec: WowSpec,
    wowClass: WowClass,
    modifier: Modifier = Modifier
) {
    val color = classColor(wowClass.id)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = WowheadIcons.medium(spec.iconName),
            contentDescription = spec.name,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = spec.name,
            color = Background,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
