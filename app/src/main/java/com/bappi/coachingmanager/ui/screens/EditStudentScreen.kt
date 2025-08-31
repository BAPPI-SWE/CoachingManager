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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bappi.coachingmanager.ui.viewmodels.EditStudentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    navController: NavController,
    viewModel: EditStudentViewModel = viewModel()
) {
    val formState = viewModel.formState
    val updateUiState = viewModel.updateUiState
    val context = LocalContext.current

    LaunchedEffect(updateUiState) {
        if (updateUiState.isSuccess) {
            Toast.makeText(context, "Student Updated!", Toast.LENGTH_SHORT).show()
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("refresh_student_list", true)
            navController.popBackStack()
            viewModel.resetUpdateState()
        }
        if (updateUiState.error != null) {
            Toast.makeText(context, "Error: ${updateUiState.error}", Toast.LENGTH_LONG).show()
            viewModel.resetUpdateState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (formState.isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
            }
        } else {
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
                    label = { Text("Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = formState.name.isBlank()
                )
                OutlinedTextField(
                    value = formState.studentClass,
                    onValueChange = { formState.studentClass = it },
                    label = { Text("Class") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.section,
                    onValueChange = { formState.section = it },
                    label = { Text("Section") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.school,
                    onValueChange = { formState.school = it },
                    label = { Text("School") },
                    modifier = Modifier.fillMaxWidth()
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
                    onClick = { viewModel.updateStudent() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !updateUiState.isSaving
                ) {
                    if (updateUiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Update Student")
                    }
                }
            }
        }
    }
}