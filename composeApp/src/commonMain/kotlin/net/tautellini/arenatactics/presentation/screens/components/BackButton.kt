package net.tautellini.arenatactics.presentation.screens.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import net.tautellini.arenatactics.presentation.theme.TextSecondary

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextSecondary)
    }
}
