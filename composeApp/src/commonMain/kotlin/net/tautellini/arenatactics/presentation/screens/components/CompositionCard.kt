package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.domain.RichComposition
import net.tautellini.arenatactics.presentation.theme.CardBorder
import net.tautellini.arenatactics.presentation.theme.CardColor
import net.tautellini.arenatactics.presentation.theme.CardElevated
import net.tautellini.arenatactics.presentation.theme.Primary

@Composable
fun CompositionCard(
    richComposition: RichComposition,
    onClick: (() -> Unit)?,         // null = hasData:false — card is unselectable
    modifier: Modifier = Modifier,
    specBadgeModifier: (specId: String) -> Modifier = { Modifier }
) {
    val hasData = richComposition.composition.hasData
    val shape = RoundedCornerShape(8.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        if (isHovered && hasData) Primary.copy(alpha = 0.25f) else CardBorder,
        animationSpec = tween(200)
    )
    val bgColor by animateColorAsState(
        if (isHovered && hasData) CardElevated else CardColor,
        animationSpec = tween(200)
    )

    Surface(
        color = bgColor,
        shape = shape,
        modifier = modifier
            .alpha(if (hasData) 1f else 0.35f)
            .hoverable(interactionSource)
            .border(1.dp, borderColor, shape)
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
            richComposition.specs.forEachIndexed { index, spec ->
                SpecBadge(
                    spec = spec,
                    wowClass = richComposition.classes[index],
                    modifier = specBadgeModifier(richComposition.composition.specIds[index])
                )
            }
        }
    }
}
