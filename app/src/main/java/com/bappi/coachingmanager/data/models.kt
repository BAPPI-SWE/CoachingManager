package com.bappi.coachingmanager.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Batch(
    val id: String = "", // Unique ID for the batch
    val name: String = "", // e.g., "Batch 1", "Morning Batch"
    val teacherId: String = "", // ID of the user who created it
    val studentCount: Int = 0, // Number of students in the batch
    @ServerTimestamp
    val createdAt: Date? = null // The time the batch was created
)

data class Student(
    val id: String = "",
    val name: String = "",
    val roll: String = "",
    val phone: String = "",
    val address: String = "",
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