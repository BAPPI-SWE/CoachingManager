package com.bappi.coachingmanager.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bappi.coachingmanager.data.Batch
import com.bappi.coachingmanager.data.Student
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BatchDetailsViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val db = Firebase.firestore

    // Get the batchId passed from the navigation
    private val batchId: String = savedStateHandle.get<String>("batchId")!!

    private val _batch = MutableStateFlow<Batch?>(null)
    val batch = _batch.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students = _students.asStateFlow()

    init {
        fetchBatchDetails()
        fetchStudents()
    }

    private fun fetchBatchDetails() {
        db.collection("batches").document(batchId)
            .addSnapshotListener { snapshot, _ ->
                _batch.value = snapshot?.toObject(Batch::class.java)
            }
    }

    private fun fetchStudents() {
        // Fetching from the 'students' subcollection of a specific batch
        db.collection("batches").document(batchId).collection("students")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _students.value = snapshot.toObjects(Student::class.java)
                }
            }
    }
}