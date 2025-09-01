package com.bappi.coachingmanager.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Student
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// UPDATE THE FORM STATE WITH NEW FIELDS
class AdmitStudentFormState {
    var name by mutableStateOf("")
    var roll by mutableStateOf("")
    var phone by mutableStateOf("")
    var address by mutableStateOf("")
    var studentClass by mutableStateOf("")
    var section by mutableStateOf("")
    var school by mutableStateOf("")
}

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

        _saveUiState.value = SaveUiState(isSaving = true)
        viewModelScope.launch {
            try {
                val student = Student(
                    name = formState.name,
                    roll = formState.roll,
                    phone = formState.phone,
                    address = formState.address,
                    // ADD THE NEW FIELDS HERE
                    studentClass = formState.studentClass,
                    section = formState.section,
                    school = formState.school,
                    // END OF NEW FIELDS
                    teacherId = userId,
                    batchId = batchId
                )

                Firebase.firestore.runBatch { batch ->
                    val newStudentDoc = Firebase.firestore
                        .collection("batches").document(batchId)
                        .collection("students").document()
                    batch.set(newStudentDoc, student.copy(id = newStudentDoc.id))

                    val batchRef = Firebase.firestore.collection("batches").document(batchId)
                    batch.update(batchRef, "studentCount", FieldValue.increment(1))
                }

                _saveUiState.value = SaveUiState(isSuccess = true)
            } catch (e: Exception) {
                _saveUiState.value = SaveUiState(error = e.message)
            }
        }
    }

    fun resetSaveState() {
        _saveUiState.value = SaveUiState()
    }
}