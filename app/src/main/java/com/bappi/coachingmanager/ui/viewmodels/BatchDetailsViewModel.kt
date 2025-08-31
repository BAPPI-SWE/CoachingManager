package com.bappi.coachingmanager.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Batch
import com.bappi.coachingmanager.data.Student
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

// Data class to hold our calculated statistics
data class PaymentStats(
    val paidCount: Int = 0,
    val unpaidCount: Int = 0
)

class BatchDetailsViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val db = Firebase.firestore
    private val batchId: String = savedStateHandle.get<String>("batchId")!!

    private val _batch = MutableStateFlow<Batch?>(null)
    val batch = _batch.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredStudents: StateFlow<List<Student>> = combine(_students, _searchQuery) { students, query ->
        if (query.isBlank()) {
            students
        } else {
            students.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(Calendar.getInstance().time)
    val selectedDate = _selectedDate.asStateFlow()

    val paymentStats: StateFlow<PaymentStats> = combine(_students, selectedDate) { studentList, date ->
        calculateStats(studentList, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentStats())

    // --- NEW: Refresh state for pull-to-refresh ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        fetchBatchDetails()
        fetchStudents()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun changeMonth(amount: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value
        calendar.add(Calendar.MONTH, amount)
        _selectedDate.value = calendar.time
    }

    private fun calculateStats(studentList: List<Student>, selectedDate: Date): PaymentStats {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        val targetMonth = calendar.get(Calendar.MONTH)
        val targetYear = calendar.get(Calendar.YEAR)

        var paidCount = 0
        studentList.forEach { student ->
            val hasPaidForMonth = student.payments.any { payment ->
                val paymentCalendar = Calendar.getInstance()
                paymentCalendar.time = payment.paymentDate
                paymentCalendar.get(Calendar.MONTH) == targetMonth &&
                        paymentCalendar.get(Calendar.YEAR) == targetYear
            }
            if (hasPaidForMonth) {
                paidCount++
            }
        }
        return PaymentStats(paidCount = paidCount, unpaidCount = studentList.size - paidCount)
    }

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            try {
                db.runBatch { batch ->
                    val studentRef = db.collection("batches").document(batchId)
                        .collection("students").document(studentId)
                    val batchRef = db.collection("batches").document(batchId)

                    batch.delete(studentRef)
                    batch.update(batchRef, "studentCount", FieldValue.increment(-1))
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    private fun fetchBatchDetails() {
        db.collection("batches").document(batchId)
            .addSnapshotListener { snapshot, _ ->
                _batch.value = snapshot?.toObject(Batch::class.java)
            }
    }

    private fun fetchStudents() {
        db.collection("batches").document(batchId).collection("students")
            .orderBy("admissionDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _students.value = snapshot.toObjects(Student::class.java)
                }
            }
    }

    // --- NEW: Refresh function for pull-to-refresh ---
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchBatchDetails()
                fetchStudents()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
