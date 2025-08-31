package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
    var showCreateDialog by remember { mutableStateOf(false) }

    // ✅ NEW: State for handling the edit and delete dialogs
    var batchToEdit by remember { mutableStateOf<Batch?>(null) }
    var batchToDelete by remember { mutableStateOf<Batch?>(null) }

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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { homeViewModel.onSearchQueryChange(it) },
                label = { Text("Search Student") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchQuery.isNotBlank()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults) { result ->
                        SearchResultCard(result = result) {
                            navController.navigate("student_details/${result.student.batchId}/${result.student.id}")
                        }
                    }
                }
            } else {
                Button(
                    onClick = { showCreateDialog = true },
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
                            BatchCard(
                                batch = batch,
                                onCardClick = { navController.navigate("batch_details/${batch.id}") },
                                onEditClick = { batchToEdit = batch },
                                onDeleteClick = { batchToDelete = batch }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateBatchDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { batchName ->
                homeViewModel.createBatch(batchName)
                showCreateDialog = false
            }
        )
    }

    // ✅ NEW: Show the Edit Batch dialog when batchToEdit is not null
    batchToEdit?.let { batch ->
        EditBatchDialog(
            batch = batch,
            onDismiss = { batchToEdit = null },
            onConfirm = { newName ->
                homeViewModel.updateBatchName(batch.id, newName)
                batchToEdit = null
            }
        )
    }

    // ✅ NEW: Show the Delete Confirmation dialog when batchToDelete is not null
    batchToDelete?.let { batch ->
        DeleteBatchDialog(
            batchName = batch.name,
            onDismiss = { batchToDelete = null },
            onConfirm = {
                homeViewModel.deleteBatch(batch.id)
                batchToDelete = null
            }
        )
    }
}

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


// ✅ MODIFIED: The BatchCard now includes a menu for edit and delete actions.
@Composable
fun BatchCard(
    batch: Batch,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = batch.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "• ${batch.studentCount} students")
            }

            // Three-dot menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Batch options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEditClick()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteClick()
                            menuExpanded = false
                        }
                    )
                }
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

// ✅ NEW: Dialog for editing a batch name.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBatchDialog(
    batch: Batch,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(batch.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Batch Name") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName)
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ✅ NEW: Dialog for confirming batch deletion.
@Composable
fun DeleteBatchDialog(
    batchName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Batch") },
        text = { Text("Are you sure you want to delete the batch '$batchName'? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}