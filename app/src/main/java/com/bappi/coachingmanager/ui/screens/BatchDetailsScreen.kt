package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bappi.coachingmanager.data.Student
import com.bappi.coachingmanager.ui.viewmodels.BatchDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailsScreen(
    navController: NavController,
    batchId: String,
    viewModel: BatchDetailsViewModel
) {

    val batch by viewModel.batch.collectAsState()
    val students by viewModel.students.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(batch?.name ?: "Batch Details") },
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
        ) {
            Text("Stats and Search will go here", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                // UPDATE THIS ONCLICK ACTION
                onClick = {
                    // Navigate to the admit screen, passing the current batchId
                    navController.navigate("admit_student/$batchId")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Admit New Student +")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("SL.", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Actions", modifier = Modifier.wrapContentWidth(), fontWeight = FontWeight.Bold)
            }
            Divider()

            if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No students admitted yet.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(students.withIndex().toList()) { (index, student) ->
                        StudentRow(serial = index + 1, student = student)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentRow(serial: Int, student: Student) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(serial.toString(), modifier = Modifier.width(40.dp))
        Text(student.name, modifier = Modifier.weight(1f), fontSize = 18.sp)

        IconButton(onClick = { /* TODO: Navigate to Payment Entry */ }) {
            Icon(Icons.Default.Edit, contentDescription = "Make Payment", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { /* TODO: Implement Delete Student */ }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
        }
    }
    Divider()
}