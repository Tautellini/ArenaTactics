package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun GearIcon(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
)
