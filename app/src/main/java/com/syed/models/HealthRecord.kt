package com.syed.models

data class HealthRecord(
    var id: String = "",
    val type: String = "", // Vaccination, Checkup, Surgery, Medication, Lab Test, Dental, Other
    val title: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val nextDueDate: Long? = null,
    val vetName: String = "",
    val vetContact: String = "",
    val cost: Double = 0.0,
    val notes: String = "",
    val attachments: List<String> = emptyList(), // URLs to documents/images
    val completed: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
