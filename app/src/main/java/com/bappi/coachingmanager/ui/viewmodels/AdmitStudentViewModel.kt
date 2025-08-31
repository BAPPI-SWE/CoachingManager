package com.bappi.coachingmanager.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Student
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// This class holds the state for our form fields
class AdmitStudentFormState {
    var name by mutableStateOf("")
    var roll by mutableStateOf("")
    var phone by mutableStateOf("")
    var address by mutableStateOf("")
}

// A new data class to represent the different UI states of the save operation
data class SaveUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AdmitStudentViewModel : ViewModel() {

    val formState = AdmitStudentFormState()

    private val _saveUiState = mutableStateOf(SaveUiState())
    val saveUiState: SaveUiState by _saveUiState

    fun saveStudent(batchId: String) {
        val userId = Firebase.auth.currentUser?.uid
        if (formState.name.isBlank() || batchId.isBlank() || userId == null) {
            _saveUiState.value = SaveUiState(error = "Name cannot be empty.")
            return
        }

        // Set state to saving
        _saveUiState.value = SaveUiState(isSaving = true)
        viewModelScope.launch {
            try {
                val newStudentDoc = Firebase.firestore
                    .collection("batches").document(batchId)
                    .collection("students").document()

                val student = Student(
                    id = newStudentDoc.id,
                    name = formState.name,
                    roll = formState.roll,
                    phone = formState.phone,
                    address = formState.address,
                    teacherId = userId,
                    batchId = batchId
                )
                newStudentDoc.set(student).await()

                // Set state to success
                _saveUiState.value = SaveUiState(isSuccess = true)
            } catch (e: Exception) {
                // Set state to error
                _saveUiState.value = SaveUiState(error = e.message)
            }
        }
    }

    // Function to reset the state after the event has been handled
    fun resetSaveState() {
        _saveUiState.value = SaveUiState()
    }
}