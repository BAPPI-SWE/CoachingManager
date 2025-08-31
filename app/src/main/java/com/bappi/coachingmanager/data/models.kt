package com.bappi.coachingmanager.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// Batch data class should be here...
data class Batch(
    val id: String = "",
    val name: String = "",
    val teacherId: String = "",
    val studentCount: Int = 0,
    @ServerTimestamp
    val createdAt: Date? = null
)


data class Student(
    val id: String = "",
    val name: String = "",
    val roll: String = "",
    val phone: String = "",
    val address: String = "",
    // ADD THESE NEW FIELDS
    val studentClass: String = "",
    val section: String = "",
    val school: String = "",
    // END OF NEW FIELDS
    val teacherId: String = "",
    val batchId: String = "",
    @ServerTimestamp
    val admissionDate: Date? = null,
    val payments: List<Payment> = emptyList()
)

data class Payment(
    val amount: Double = 0.0,
    val paymentDate: Date = Date(),
    val notes: String = ""
)
// ADD THIS SHARED HELPER CLASS HERE
data class SaveUiState(
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)