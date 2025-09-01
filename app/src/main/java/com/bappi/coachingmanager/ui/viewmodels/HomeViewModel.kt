package com.bappi.coachingmanager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Batch
import com.bappi.coachingmanager.data.Student
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data class to combine Student and their Batch Name for search results
data class StudentSearchResult(
    val student: Student,
    val batchName: String
)

// Dashboard statistics data class
data class DashboardStats(
    val monthlyCollection: Double = 0.0,
    val yearlyCollection: Double = 0.0,
    val totalStudents: Int = 0,
    val monthlyPaidStudents: Int = 0,
    val monthlyUnpaidStudents: Int = 0
)

// Payment summary for dialog display
data class PaymentSummary(
    val period: String = "",
    val amount: Double = 0.0,
    val studentName: String = "",
    val batchName: String = ""
)

class HomeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches = _batches.asStateFlow()

    private val _allStudents = MutableStateFlow<List<Student>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<StudentSearchResult>> =
        combine(_allStudents, batches, _searchQuery) { students, batchList, query ->
            if (query.isBlank()) {
                emptyList()
            } else {
                students
                    .filter { it.name.contains(query, ignoreCase = true) }
                    .mapNotNull { student ->
                        val batch = batchList.find { it.id == student.batchId }
                        batch?.let { StudentSearchResult(student, it.name) }
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard statistics
    val dashboardStats: StateFlow<DashboardStats> =
        combine(_allStudents, batches) { students, _ ->
            calculateDashboardStats(students)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        fetchBatchesAndStudents()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun fetchBatchesAndStudents() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("batches")
            .whereEqualTo("teacherId", userId)
            .orderBy("createdAt")
            .addSnapshotListener { batchSnapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching batches: ", error)
                    return@addSnapshotListener
                }
                if (batchSnapshot != null) {
                    val batchList = batchSnapshot.toObjects(Batch::class.java)
                    _batches.value = batchList
                    fetchAllStudents(userId)
                }
            }
    }

    private fun fetchAllStudents(userId: String) {
        if (userId.isBlank()) return
        db.collectionGroup("students").whereEqualTo("teacherId", userId)
            .addSnapshotListener { studentSnapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Error fetching all students: ", error)
                    return@addSnapshotListener
                }
                if (studentSnapshot != null) {
                    _allStudents.value = studentSnapshot.toObjects(Student::class.java)
                }
            }
    }

    private fun calculateDashboardStats(students: List<Student>): DashboardStats {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate

        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var monthlyCollection = 0.0
        var yearlyCollection = 0.0
        var monthlyPaidStudents = 0
        val totalStudents = students.size

        students.forEach { student ->
            var hasMonthlyPayment = false

            student.payments.forEach { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                val paymentMonth = paymentCalendar.get(Calendar.MONTH)
                val paymentYear = paymentCalendar.get(Calendar.YEAR)

                // Monthly collection
                if (paymentMonth == currentMonth && paymentYear == currentYear) {
                    monthlyCollection += payment.amount
                    hasMonthlyPayment = true
                }

                // Yearly collection
                if (paymentYear == currentYear) {
                    yearlyCollection += payment.amount
                }
            }

            if (hasMonthlyPayment) {
                monthlyPaidStudents++
            }
        }

        val monthlyUnpaidStudents = totalStudents - monthlyPaidStudents

        return DashboardStats(
            monthlyCollection = monthlyCollection,
            yearlyCollection = yearlyCollection,
            totalStudents = totalStudents,
            monthlyPaidStudents = monthlyPaidStudents,
            monthlyUnpaidStudents = monthlyUnpaidStudents
        )
    }

    fun getSummaryData(statType: String): StateFlow<List<PaymentSummary>> {
        return combine(_allStudents, _batches) { students, batchList ->
            when (statType) {
                "monthly_collection" -> getMonthlyCollectionSummary(students)
                "yearly_collection" -> getYearlyCollectionSummary(students)
                "total_students" -> getTotalStudentsSummary(students, batchList)
                "paid_students" -> getPaidStudentsSummary(students, batchList)
                "unpaid_students" -> getUnpaidStudentsSummary(students, batchList)
                else -> emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private fun getMonthlyCollectionSummary(students: List<Student>): List<PaymentSummary> {
        val monthlyData = mutableMapOf<String, Double>()
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        students.forEach { student ->
            student.payments.forEach { payment ->
                val monthKey = dateFormat.format(payment.paymentDate)
                monthlyData[monthKey] = monthlyData.getOrDefault(monthKey, 0.0) + payment.amount
            }
        }

        return monthlyData.map { (month, amount) ->
            PaymentSummary(period = month, amount = amount)
        }.sortedByDescending { it.period }
    }

    private fun getYearlyCollectionSummary(students: List<Student>): List<PaymentSummary> {
        val yearlyData = mutableMapOf<String, Double>()
        val calendar = Calendar.getInstance()

        students.forEach { student ->
            student.payments.forEach { payment ->
                calendar.time = payment.paymentDate
                val year = calendar.get(Calendar.YEAR).toString()
                yearlyData[year] = yearlyData.getOrDefault(year, 0.0) + payment.amount
            }
        }

        return yearlyData.map { (year, amount) ->
            PaymentSummary(period = year, amount = amount)
        }.sortedByDescending { it.period }
    }

    private fun getTotalStudentsSummary(students: List<Student>, batches: List<Batch>): List<PaymentSummary> {
        return students.map { student ->
            val batch = batches.find { it.id == student.batchId }
            val lastPayment = student.payments.maxByOrNull { it.paymentDate }

            PaymentSummary(
                studentName = student.name,
                batchName = batch?.name ?: "Unknown Batch",
                amount = lastPayment?.amount ?: 0.0
            )
        }.sortedBy { it.studentName }
    }

    private fun getPaidStudentsSummary(students: List<Student>, batches: List<Batch>): List<PaymentSummary> {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return students.filter { student ->
            student.payments.any { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                paymentCalendar.get(Calendar.MONTH) == currentMonth &&
                        paymentCalendar.get(Calendar.YEAR) == currentYear
            }
        }.map { student ->
            val batch = batches.find { it.id == student.batchId }
            val monthlyPayments = student.payments.filter { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                paymentCalendar.get(Calendar.MONTH) == currentMonth &&
                        paymentCalendar.get(Calendar.YEAR) == currentYear
            }
            val totalMonthlyAmount = monthlyPayments.sumOf { it.amount }

            PaymentSummary(
                studentName = student.name,
                batchName = batch?.name ?: "Unknown Batch",
                amount = totalMonthlyAmount
            )
        }.sortedBy { it.studentName }
    }

    private fun getUnpaidStudentsSummary(students: List<Student>, batches: List<Batch>): List<PaymentSummary> {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return students.filter { student ->
            !student.payments.any { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                paymentCalendar.get(Calendar.MONTH) == currentMonth &&
                        paymentCalendar.get(Calendar.YEAR) == currentYear
            }
        }.map { student ->
            val batch = batches.find { it.id == student.batchId }
            val lastPayment = student.payments.maxByOrNull { it.paymentDate }

            PaymentSummary(
                studentName = student.name,
                batchName = batch?.name ?: "Unknown Batch",
                amount = lastPayment?.amount ?: 0.0
            )
        }.sortedBy { it.studentName }
    }

    fun createBatch(batchName: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val newBatchRef = db.collection("batches").document()
                val newBatch = Batch(
                    id = newBatchRef.id,
                    name = batchName,
                    teacherId = userId,
                    studentCount = 0
                )
                newBatchRef.set(newBatch)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error creating batch: ", e)
            }
        }
    }

    fun updateBatchName(batchId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("batches").document(batchId)
                    .update("name", newName)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating batch name: ", e)
            }
        }
    }

    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            try {
                // Note: This is a simplified delete. For production, you might want to
                // delete all students in the batch in a batched write for safety.
                db.collection("batches").document(batchId).delete()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting batch: ", e)
            }
        }
    }
}