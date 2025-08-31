package com.bappi.coachingmanager.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bappi.coachingmanager.data.Student
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudentDetailsViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val batchId: String = savedStateHandle.get<String>("batchId")!!
    private val studentId: String = savedStateHandle.get<String>("studentId")!!

    private val _student = MutableStateFlow<Student?>(null)
    val student = _student.asStateFlow()

    init {
        fetchStudentDetails()
    }

    private fun fetchStudentDetails() {
        if (batchId.isBlank() || studentId.isBlank()) return

        val studentDocRef = Firebase.firestore
            .collection("batches").document(batchId)
            .collection("students").document(studentId)

        studentDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("StudentDetailsVM", "Error fetching student details", error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                _student.value = snapshot.toObject(Student::class.java)
            }
        }
    }
}