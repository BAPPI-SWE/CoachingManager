package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bappi.coachingmanager.data.Batch
import com.bappi.coachingmanager.ui.viewmodels.HomeViewModel
import com.bappi.coachingmanager.ui.viewmodels.StudentSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, homeViewModel: HomeViewModel = viewModel()) {

    val batches by homeViewModel.batches.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val searchResults by homeViewModel.searchResults.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar is now connected to the ViewModel
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { homeViewModel.onSearchQueryChange(it) },
                label = { Text("Search Student") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CONDITIONAL UI: Show search results or the batch list
            if (searchQuery.isNotBlank()) {
                // Show Search Results
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults) { result ->
                        SearchResultCard(result = result) {
                            // Navigate directly to student details on click
                            navController.navigate("student_details/${result.student.batchId}/${result.student.id}")
                        }
                    }
                }
            } else {
                // Show Batch List (the original UI)
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Create New Batch +")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (batches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No batches found. Create one!")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(batches) { batch ->
                            BatchCard(batch = batch) {
                                navController.navigate("batch_details/${batch.id}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CreateBatchDialog(
            onDismiss = { showDialog = false },
            onCreate = { batchName ->
                homeViewModel.createBatch(batchName)
                showDialog = false
            }
        )
    }
}

// NEW: A composable for displaying a single search result item
@Composable
fun SearchResultCard(result: StudentSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(result.student.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Batch: ${result.batchName}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


// BatchCard and CreateBatchDialog composables remain the same...
@Composable
fun BatchCard(batch: Batch, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = batch.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "• ${batch.studentCount} students")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var batchName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Batch") },
        text = {
            OutlinedTextField(
                value = batchName,
                onValueChange = { batchName = it },
                label = { Text("Batch Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (batchName.isNotBlank()) {
                        onCreate(batchName)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}