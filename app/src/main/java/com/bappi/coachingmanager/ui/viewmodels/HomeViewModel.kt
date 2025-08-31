package com.bappi.coachingmanager.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Batch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _batches = MutableStateFlow<List<Batch>>(emptyList())
    val batches = _batches.asStateFlow()

    init {
        fetchBatches()
    }

    private fun fetchBatches() {
        val userId = auth.currentUser?.uid ?: return // Exit if user is not logged in

        // Listen for real-time updates from Firestore
        db.collection("batches")
            .whereEqualTo("teacherId", userId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val batchList = snapshot.toObjects(Batch::class.java)
                    _batches.value = batchList
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
                    // createdAt is handled by @ServerTimestamp
                )
                newBatchRef.set(newBatch).await()
                // The list will update automatically because of the snapshot listener
            } catch (e: Exception) {
                // Handle error, e.g., show a toast
            }
        }
    }
}