package com.syed.domain.model

import com.google.firebase.Timestamp

data class Pet(
    val id: String = "",
    val name: String = "",
    val type: String = "", // dog, cat, other
    val breed: String = "",
    val age: String = "", // Changed from Int to String for consistency
    val gender: String = "",
    val description: String = "",
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val available: Boolean = true, // Single availability field
    val location: String = "",
    val addedBy: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    // Additional fields from Firestore
    val personalityTraits: List<String> = emptyList(),
    val color: String = "",
    val vaccinated: Boolean = false,
    val ownerId: String = "",
    val specialNeeds: String = "",
    val createdAt: Timestamp? = null,
    val imageUrl: String = "", // Single image URL (legacy field)
    val adoptionFee: Double = 0.0,
    val contactNumber: String = "",
    val medicalHistory: String = "",
    val stability: String = "",
    val updatedAt: Timestamp? = null,
    val weight: Double = 0.0,
    val neutered: Boolean = false,
    val temperament: String = "",
    val shelterName: String = "",
    val category: String = "",
    val adopted: Boolean = false,
    val actuallyAvailable: Boolean = true, // Added missing field
) {
    // Single computed property to check availability (no conflicting getters)
    fun isCurrentlyAvailable(): Boolean = available && !adopted && actuallyAvailable
}
