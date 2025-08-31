package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bappi.coachingmanager.Routes
import com.bappi.coachingmanager.auth.GoogleAuthUiClient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    googleAuthUiClient: GoogleAuthUiClient
) {
    // This effect runs once when the screen is composed
    LaunchedEffect(key1 = true) {
        // Wait for 2 seconds
        delay(2000L)

        // Check if user is already signed in
        val destination = if (googleAuthUiClient.getSignedInUser() != null) {
            Routes.HOME_SCREEN
        } else {
            Routes.LOGIN_SCREEN
        }

        // Navigate to the correct screen
        navController.navigate(destination) {
            // This removes the splash screen from the back stack, so the user
            // can't press the back button to return to it.
            popUpTo(Routes.SPLASH_SCREEN) {
                inclusive = true
            }
        }
    }

    // The UI of the splash screen
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(modifier = Modifier.padding(26.dp),
            text = "Welcome to Kamruzzaman's Coaching management App",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}