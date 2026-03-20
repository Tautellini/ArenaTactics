package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.domain.RichComposition
import net.tautellini.arenatactics.presentation.theme.CardColor

@Composable
fun CompositionCard(
    richComposition: RichComposition,
    onClick: (() -> Unit)?,         // null = hasData:false — card is unselectable
    modifier: Modifier = Modifier
) {
    val hasData = richComposition.composition.hasData
    Surface(
        color = CardColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .alpha(if (hasData) 1f else 0.35f)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecBadge(richComposition.specs[0], richComposition.classes[0])
            SpecBadge(richComposition.specs[1], richComposition.classes[1])
        }
    }
}
