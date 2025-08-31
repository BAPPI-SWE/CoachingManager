package com.bappi.coachingmanager.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.SaveUiState
import com.bappi.coachingmanager.data.Student
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// A state holder for the edit form fields
class EditStudentFormState {
    var name by mutableStateOf("")
    var roll by mutableStateOf("")
    var phone by mutableStateOf("")
    var address by mutableStateOf("")
    var studentClass by mutableStateOf("")
    var section by mutableStateOf("")
    var school by mutableStateOf("")
    var isLoading by mutableStateOf(true)
}

class EditStudentViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val batchId: String = savedStateHandle.get<String>("batchId")!!
    private val studentId: String = savedStateHandle.get<String>("studentId")!!

    val formState = EditStudentFormState()

    private val _updateUiState = mutableStateOf(SaveUiState())
    val updateUiState: SaveUiState by _updateUiState

    private val studentDocRef = Firebase.firestore
        .collection("batches").document(batchId)
        .collection("students").document(studentId)

    init {
        fetchStudentData()
    }

    private fun fetchStudentData() {
        viewModelScope.launch {
            try {
                val document = studentDocRef.get().await()
                val student = document.toObject(Student::class.java)
                student?.let {
                    formState.name = it.name
                    formState.roll = it.roll
                    formState.phone = it.phone
                    formState.address = it.address
                    formState.studentClass = it.studentClass
                    formState.section = it.section
                    formState.school = it.school
                }
            } catch (e: Exception) {
                _updateUiState.value = SaveUiState(error = "Failed to load student data.")
            } finally {
                formState.isLoading = false
            }
        }
    }

    fun updateStudent() {
        if (formState.name.isBlank()) {
            _updateUiState.value = SaveUiState(error = "Name cannot be empty.")
            return
        }

        _updateUiState.value = SaveUiState(isSaving = true)
        viewModelScope.launch {
            try {
                val updatedData = mapOf(
                    "name" to formState.name,
                    "roll" to formState.roll,
                    "phone" to formState.phone,
                    "address" to formState.address,
                    "studentClass" to formState.studentClass,
                    "section" to formState.section,
                    "school" to formState.school
                )
                studentDocRef.update(updatedData).await()
                _updateUiState.value = SaveUiState(isSuccess = true)
            } catch (e: Exception) {
                _updateUiState.value = SaveUiState(error = e.message)
            }
        }
    }

    fun resetUpdateState() {
        _updateUiState.value = SaveUiState()
    }
}