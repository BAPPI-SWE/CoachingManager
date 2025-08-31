package com.bappi.coachingmanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bappi.coachingmanager.ui.viewmodels.AdmitStudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmitStudentScreen(
    navController: NavController,
    batchId: String,
    viewModel: AdmitStudentViewModel = viewModel()
) {
    val formState = viewModel.formState
    val saveUiState = viewModel.saveUiState
    val context = LocalContext.current

    // This effect will now handle both success and error states
    LaunchedEffect(saveUiState) {
        if (saveUiState.isSuccess) {
            Toast.makeText(context, "Student Saved!", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            viewModel.resetSaveState() // Reset state after handling
        }
        if (saveUiState.error != null) {
            Toast.makeText(context, "Error: ${saveUiState.error}", Toast.LENGTH_LONG).show()
            viewModel.resetSaveState() // Reset state after handling
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admit New Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = formState.name,
                onValueChange = { formState.name = it },
                label = { Text("Name*") }, // Mark as required
                modifier = Modifier.fillMaxWidth(),
                isError = formState.name.isBlank()
            )
            OutlinedTextField(
                value = formState.roll,
                onValueChange = { formState.roll = it },
                label = { Text("Roll") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formState.phone,
                onValueChange = { formState.phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = formState.address,
                onValueChange = { formState.address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveStudent(batchId) },
                modifier = Modifier.fillMaxWidth(),
                // Disable the button while saving to prevent multiple clicks
                enabled = !saveUiState.isSaving
            ) {
                if (saveUiState.isSaving) {
                    // Show a loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Student")
                }
            }
        }
    }
}