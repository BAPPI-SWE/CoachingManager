package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bappi.coachingmanager.data.Student
import com.bappi.coachingmanager.ui.viewmodels.BatchDetailsViewModel
import com.bappi.coachingmanager.ui.viewmodels.PaymentStats
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailsScreen(
    navController: NavController,
    batchId: String,
    viewModel: BatchDetailsViewModel
) {
    val batch by viewModel.batch.collectAsState()
    val students by viewModel.students.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val stats by viewModel.paymentStats.collectAsState()

    // State to control the delete confirmation dialog
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

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
            // New Stats Section
            StatsSection(
                stats = stats,
                selectedDate = selectedDate,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("admit_student/$batchId") },
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
                        StudentRow(
                            serial = index + 1,
                            student = student,
                            navController = navController,
                            onDeleteClick = { studentToDelete = student } // Trigger dialog
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Deletion
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${student.name}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(student.id)
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatsSection(
    stats: PaymentStats,
    selectedDate: Date,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("✅ Paid: ${stats.paidCount}", color = MaterialTheme.colorScheme.primary)
                Text("❌ Unpaid: ${stats.unpaidCount}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun StudentRow(
    serial: Int,
    student: Student,
    navController: NavController,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("student_details/${student.batchId}/${student.id}") }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(serial.toString(), modifier = Modifier.width(40.dp))
        Text(student.name, modifier = Modifier.weight(1f), fontSize = 18.sp)

        IconButton(onClick = { navController.navigate("payment_entry/${student.batchId}/${student.id}") }) {
            Icon(Icons.Default.Edit, contentDescription = "Make Payment", tint = MaterialTheme.colorScheme.primary)
        }
        // Connect the delete click handler
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
        }
    }
    Divider()
}