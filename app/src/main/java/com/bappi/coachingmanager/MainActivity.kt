package com.bappi.coachingmanager

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bappi.coachingmanager.auth.GoogleAuthUiClient
import com.bappi.coachingmanager.ui.screens.*
import com.bappi.coachingmanager.ui.theme.CoachingManagerTheme
import com.bappi.coachingmanager.ui.viewmodels.LoginViewModel
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoachingManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Routes.SPLASH_SCREEN) {
                        composable(Routes.SPLASH_SCREEN) {
                            SplashScreen(
                                navController = navController,
                                googleAuthUiClient = googleAuthUiClient
                            )
                        }

                        composable(Routes.LOGIN_SCREEN) {
                            // ... Login Screen Logic
                            val viewModel = viewModel<LoginViewModel>()
                            val state by viewModel.state.collectAsStateWithLifecycle()

                            LaunchedEffect(key1 = Unit) {
                                if(googleAuthUiClient.getSignedInUser() != null) {
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                                    }
                                }
                            }
                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == RESULT_OK) {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val signInResult = googleAuthUiClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            viewModel.onSignInResult(signInResult)
                                        }
                                    }
                                }
                            )
                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                                    }
                                    viewModel.resetState()
                                }
                            }
                            LoginScreen(
                                state = state,
                                onSignInClick = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val signInIntentSender = googleAuthUiClient.signIn()
                                        launcher.launch(
                                            IntentSenderRequest.Builder(
                                                signInIntentSender ?: return@launch
                                            ).build()
                                        )
                                    }
                                }
                            )
                        }
                        composable(Routes.HOME_SCREEN) {
                            HomeScreen(navController = navController)
                        }
                        composable(
                            route = Routes.BATCH_DETAILS_SCREEN,
                            arguments = listOf(navArgument("batchId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
                            BatchDetailsScreen(
                                navController = navController,
                                batchId = batchId,
                                viewModel = viewModel()
                            )
                        }
                        composable(
                            route = Routes.ADMIT_STUDENT_SCREEN,
                            arguments = listOf(navArgument("batchId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val batchId = backStackEntry.arguments?.getString("batchId") ?: ""
                            AdmitStudentScreen(
                                navController = navController,
                                batchId = batchId
                            )
                        }
                        composable(
                            route = Routes.PAYMENT_ENTRY_SCREEN,
                            arguments = listOf(
                                navArgument("batchId") { type = NavType.StringType },
                                navArgument("studentId") { type = NavType.StringType }
                            )
                        ) {
                            PaymentEntryScreen(navController = navController, viewModel = viewModel())
                        }
                        composable(
                            route = Routes.STUDENT_DETAILS_SCREEN,
                            arguments = listOf(
                                navArgument("batchId") { type = NavType.StringType },
                                navArgument("studentId") { type = NavType.StringType }
                            )
                        ) {
                            StudentDetailsScreen(navController = navController, viewModel = viewModel())
                        }
                        // ✅ ADD THIS NEW COMPOSABLE BLOCK FOR THE EDIT SCREEN
                        composable(
                            route = Routes.EDIT_STUDENT_SCREEN,
                            arguments = listOf(
                                navArgument("batchId") { type = NavType.StringType },
                                navArgument("studentId") { type = NavType.StringType }
                            )
                        ) {
                            EditStudentScreen(navController = navController, viewModel = viewModel())
                        }
                    }
                }
            }
        }
    }
}

object Routes {
    const val SPLASH_SCREEN = "splash"

    const val LOGIN_SCREEN = "login"
    const val HOME_SCREEN = "home"
    const val BATCH_DETAILS_SCREEN = "batch_details/{batchId}"
    const val ADMIT_STUDENT_SCREEN = "admit_student/{batchId}"
    const val PAYMENT_ENTRY_SCREEN = "payment_entry/{batchId}/{studentId}"
    const val STUDENT_DETAILS_SCREEN = "student_details/{batchId}/{studentId}"
    // ✅ ADD THIS NEW ROUTE DEFINITION
    const val EDIT_STUDENT_SCREEN = "edit_student/{batchId}/{studentId}"
}