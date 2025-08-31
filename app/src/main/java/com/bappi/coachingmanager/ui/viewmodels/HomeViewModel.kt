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
import kotlinx.coroutines.tasks.await

// Data class to combine Student and their Batch Name for search results
data class StudentSearchResult(
    val student: Student,
    val batchName: String
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
                newBatchRef.set(newBatch).await()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error creating batch: ", e)
            }
        }
    }

    // ✅ NEW: Function to update a batch's name
    fun updateBatchName(batchId: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                db.collection("batches").document(batchId)
                    .update("name", newName)
                    .await()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error updating batch name: ", e)
            }
        }
    }

    // ✅ NEW: Function to delete a batch (and its students)
    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            try {
                // Note: This is a simplified delete. For production, you might want to
                // delete all students in the batch in a batched write for safety.
                db.collection("batches").document(batchId).delete().await()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error deleting batch: ", e)
            }
        }
    }
}