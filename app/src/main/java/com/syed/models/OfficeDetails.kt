package com.syed.models

data class OfficeDetails(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val workingHours: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val addedBy: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
