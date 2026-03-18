package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tautellini.arenatactics.presentation.theme.TextSecondary

@Composable
fun BackButton(onClick: () -> Unit) {
    Text(
        text = "← Back",
        color = TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    )
}
