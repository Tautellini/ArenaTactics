package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import net.tautellini.arenatactics.presentation.theme.CardElevated
import net.tautellini.arenatactics.presentation.theme.TextSecondary

@Composable
actual fun GearIcon(
    url: String,
    contentDescription: String,
    modifier: Modifier,
    cornerRadius: Dp,
    borderColor: Color,
    borderWidth: Dp,
    wowheadItemId: Int
) {
    val shape = RoundedCornerShape(cornerRadius)
    val m = modifier
        .clip(shape)
        .then(
            if (borderWidth > 0.dp && borderColor != Color.Transparent)
                Modifier.border(borderWidth, borderColor, shape)
            else Modifier
        )
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = m,
        error = {
            Box(
                Modifier.fillMaxSize().background(CardElevated),
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = TextSecondary, fontSize = 18.sp)
            }
        }
    )
}
