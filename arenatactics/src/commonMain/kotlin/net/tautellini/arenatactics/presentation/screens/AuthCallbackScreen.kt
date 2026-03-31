package net.tautellini.arenatactics.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.tautellini.arenatactics.getQueryParams
import net.tautellini.arenatactics.presentation.AuthViewModel
import net.tautellini.arenatactics.presentation.theme.Background
import net.tautellini.arenatactics.presentation.theme.Primary

@Composable
fun AuthCallbackScreen(
    authViewModel: AuthViewModel,
    onNavigateHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        val params = getQueryParams()
        val token = params["token"]
        val refresh = params["refresh"]

        if (token != null && refresh != null) {
            authViewModel.handleCallback(token, refresh)
        }

        onNavigateHome()
    }

    Box(
        Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
    }
}
