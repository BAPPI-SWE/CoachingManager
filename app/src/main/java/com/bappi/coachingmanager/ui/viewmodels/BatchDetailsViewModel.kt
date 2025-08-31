package com.bappi.coachingmanager.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Batch
import com.bappi.coachingmanager.data.Student
import com.google.firebase.firestore.FieldValue
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
    val students = _students.asStateFlow()

    // State for the selected month and year for stats
    private val _selectedDate = MutableStateFlow(Calendar.getInstance().time)
    val selectedDate = _selectedDate.asStateFlow()

    // State to hold the calculated payment stats
    val paymentStats: StateFlow<PaymentStats> = combine(students, selectedDate) { studentList, date ->
        calculateStats(studentList, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaymentStats())


    init {
        fetchBatchDetails()
        fetchStudents()
    }

    // Function to change the month for the stats
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
                // We use a batch write to delete the student AND decrement the
                // studentCount on the batch document in a single, atomic operation.
                db.runBatch { batch ->
                    val studentRef = db.collection("batches").document(batchId)
                        .collection("students").document(studentId)
                    val batchRef = db.collection("batches").document(batchId)

                    batch.delete(studentRef)
                    batch.update(batchRef, "studentCount", FieldValue.increment(-1))
                }
            } catch (e: Exception) {
                // Handle error
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
            .orderBy("name") // Let's sort students by name
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _students.value = snapshot.toObjects(Student::class.java)
                }
            }
    }
}