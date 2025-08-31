package com.bappi.coachingmanager.ui.viewmodels

import android.util.Log
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
        val userId = auth.currentUser?.uid ?: return

        db.collection("batches")
            .whereEqualTo("teacherId", userId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                // THIS IS THE UPDATED PART
                if (error != null) {
                    // We added this Log statement to see the error in Logcat
                    Log.e("HomeViewModel", "Error fetching batches: ", error)
                    return@addSnapshotListener
                }
                // END OF UPDATED PART
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
                )
                newBatchRef.set(newBatch).await()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error creating batch: ", e)
            }
        }
    }
}