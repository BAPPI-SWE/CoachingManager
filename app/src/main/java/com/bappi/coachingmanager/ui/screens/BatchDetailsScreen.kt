package com.bappi.coachingmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bappi.coachingmanager.data.Student
import com.bappi.coachingmanager.ui.viewmodels.BatchDetailsViewModel
import com.bappi.coachingmanager.ui.viewmodels.PaymentStats
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.*

private enum class StudentListType { PAID, UNPAID }
private data class StudentPaymentInfo(val student: Student, val amountPaid: Double)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailsScreen(
    navController: NavController,
    batchId: String,
    viewModel: BatchDetailsViewModel
) {
    val batch by viewModel.batch.collectAsState()
    val students by viewModel.filteredStudents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val stats by viewModel.paymentStats.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var listToShow by remember { mutableStateOf<StudentListType?>(null) }

    // ✅ LISTEN for the result from the PaymentEntryScreen.
    val navBackStackEntry = navController.currentBackStackEntry
    val paymentMade = navBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Boolean>("payment_successful")
        ?.observeAsState()

    LaunchedEffect(paymentMade) {
        if (paymentMade?.value == true) {
            viewModel.refresh()
            // Reset the value to prevent multiple refreshes
            navBackStackEntry?.savedStateHandle?.remove<Boolean>("payment_successful")
        }
    }

    // This logic creates the detailed lists for the dialogs.
    val (paidStudentsInfo, unpaidStudents) = remember(students, selectedDate) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)

        val paid = mutableListOf<StudentPaymentInfo>()
        val unpaid = mutableListOf<Student>()

        students.forEach { student ->
            val paymentsInMonth = student.payments.filter { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                paymentCalendar.get(Calendar.MONTH) == targetMonth &&
                        paymentCalendar.get(Calendar.YEAR) == targetYear
            }

            if (paymentsInMonth.isNotEmpty()) {
                val totalAmount = paymentsInMonth.sumOf { it.amount }
                paid.add(StudentPaymentInfo(student, totalAmount))
            } else {
                unpaid.add(student)
            }
        }
        paid to unpaid
    }

    // Create a set of paid student IDs for efficient lookup to highlight rows.
    val paidStudentIds = remember(students, selectedDate) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)

        students.filter { student ->
            student.payments.any { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                paymentCalendar.get(Calendar.MONTH) == targetMonth &&
                        paymentCalendar.get(Calendar.YEAR) == targetYear
            }
        }.map { it.id }.toSet()
    }


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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by name...") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            StatsSection(
                stats = stats,
                selectedDate = selectedDate,
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) },
                onPaidClick = { listToShow = StudentListType.PAID },
                onUnpaidClick = { listToShow = StudentListType.UNPAID }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("admit_student/$batchId") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Admit New Student +")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)) {
                Text("SL.", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                Text("Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Actions", modifier = Modifier.wrapContentWidth(), fontWeight = FontWeight.Bold)
            }
            Divider()

            val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (students.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No students found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(students.withIndex().toList()) { (index, student) ->
                            val isPaid = student.id in paidStudentIds
                            StudentRow(
                                serial = index + 1,
                                student = student,
                                navController = navController,
                                isPaid = isPaid,
                                onDeleteClick = { studentToDelete = student }
                            )
                        }
                    }
                }
            }
        }
    }

    listToShow?.let { type ->
        val title: String
        val studentsForDialog: List<Any>

        if (type == StudentListType.PAID) {
            title = "Paid Students"
            studentsForDialog = paidStudentsInfo
        } else {
            title = "Unpaid Students"
            studentsForDialog = unpaidStudents
        }

        StudentListDialog(
            title = title,
            students = studentsForDialog,
            onDismiss = { listToShow = null }
        )
    }


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
fun StudentListDialog(
    title: String,
    students: List<Any>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (students.isEmpty()) {
                Text("No students in this category.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(students) { item ->
                        Column {
                            when (item) {
                                is StudentPaymentInfo -> {
                                    val student = item.student
                                    val displayText = if (student.roll.isNotBlank()) "${student.roll}. ${student.name}" else student.name
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = displayText)
                                        Text(
                                            text = "৳${item.amountPaid}",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                is Student -> {
                                    val displayText = if (item.roll.isNotBlank()) "${item.roll}. ${item.name}" else item.name
                                    Text(
                                        text = displayText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun StatsSection(
    stats: PaymentStats,
    selectedDate: Date,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPaidClick: () -> Unit,
    onUnpaidClick: () -> Unit
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
                Text(
                    text = "✅ Paid: ${stats.paidCount}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onPaidClick)
                )
                Text(
                    text = "❌ Unpaid: ${stats.unpaidCount}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = onUnpaidClick)
                )
            }
        }
    }
}

@Composable
fun StudentRow(
    serial: Int,
    student: Student,
    navController: NavController,
    isPaid: Boolean,
    onDeleteClick: () -> Unit
) {
    val rowColor = if (isPaid) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = rowColor)
            .clickable { navController.navigate("student_details/${student.batchId}/${student.id}") }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(serial.toString(), modifier = Modifier.width(40.dp))
        Text(student.name, modifier = Modifier.weight(1f), fontSize = 18.sp)

        Button(
            onClick = { navController.navigate("payment_entry/${student.batchId}/${student.id}") },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text("Pay", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
        }
    }
    Divider()
}