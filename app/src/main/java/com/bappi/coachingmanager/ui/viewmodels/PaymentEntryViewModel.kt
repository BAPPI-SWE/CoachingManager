package com.bappi.coachingmanager.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bappi.coachingmanager.data.Payment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class PaymentEntryViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val batchId: String = savedStateHandle.get<String>("batchId")!!
    val studentId: String = savedStateHandle.get<String>("studentId")!!

    var amount by mutableStateOf("")
    var paymentDate by mutableStateOf(Date()) // Defaults to now

    private val _saveUiState = mutableStateOf(SaveUiState())
    val saveUiState: SaveUiState by _saveUiState

    fun savePayment() {
        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue <= 0) {
            _saveUiState.value = SaveUiState(error = "Please enter a valid amount.")
            return
        }

        _saveUiState.value = SaveUiState(isSaving = true)
        viewModelScope.launch {
            try {
                val newPayment = Payment(
                    amount = amountValue,
                    paymentDate = paymentDate
                )

                val studentDocRef = Firebase.firestore
                    .collection("batches").document(batchId)
                    .collection("students").document(studentId)

                // This command adds the new payment to the 'payments' array
                // without overwriting existing payments.
                studentDocRef.update("payments", FieldValue.arrayUnion(newPayment))

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