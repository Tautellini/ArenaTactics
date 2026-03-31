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
import net.tautellini.arenatactics.getCurrentPath
import net.tautellini.arenatactics.navigation.Screen
import net.tautellini.arenatactics.presentation.AuthViewModel
import net.tautellini.arenatactics.presentation.theme.Background
import net.tautellini.arenatactics.presentation.theme.Primary

/**
 * Handles the OAuth redirect callback. Parses token and refreshToken
 * from the URL query parameters, stores them, and navigates to dashboard.
 */
@Composable
fun AuthCallbackScreen(
    authViewModel: AuthViewModel,
    onNavigateHome: () -> Unit
) {
    LaunchedEffect(Unit) {
        // Parse tokens from URL query string
        val path = getCurrentPath()
        val queryString = path.substringAfter("?", "")
        val params = queryString.split("&").associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }

        val token = params["token"]
        val refresh = params["refresh"]

        if (token != null && refresh != null) {
            authViewModel.handleCallback(token, refresh)
        }

        // Navigate to dashboard regardless
        onNavigateHome()
    }

    // Show loading while processing
    Box(
        Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Primary, strokeWidth = 2.dp)
    }
}
