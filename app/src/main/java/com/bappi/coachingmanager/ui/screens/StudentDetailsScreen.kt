package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bappi.coachingmanager.data.Payment
import com.bappi.coachingmanager.ui.viewmodels.StudentDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsScreen(
    navController: NavController,
    viewModel: StudentDetailsViewModel = viewModel()
) {
    val student by viewModel.student.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student?.name ?: "Student Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        student?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ADD THE NEW FIELDS TO THE DISPLAY
                DetailItem(label = "Class:", value = it.studentClass)
                DetailItem(label = "Section:", value = it.section)
                DetailItem(label = "School:", value = it.school)
                Divider() // Add a separator
                Spacer(modifier = Modifier.height(8.dp))
                // END OF NEW FIELDS

                DetailItem(label = "Roll:", value = it.roll)
                DetailItem(label = "Phone:", value = it.phone)
                DetailItem(label = "Address:", value = it.address)

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Transaction History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Divider()

                if (it.payments.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No transactions found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(it.payments.sortedByDescending { p -> p.paymentDate }) { payment ->
                            TransactionRow(payment = payment)
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

// DetailItem and TransactionRow composables remain the same
@Composable
fun DetailItem(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Text(value)
    }
}

@Composable
fun TransactionRow(payment: Payment) {
    val dateFormatter = SimpleDateFormat("dd-MM-yyyy, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dateFormatter.format(payment.paymentDate))
            Text("${payment.amount} Tk", fontWeight = FontWeight.Bold)
        }
    }
}