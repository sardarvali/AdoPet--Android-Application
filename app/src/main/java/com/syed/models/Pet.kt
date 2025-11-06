package com.syed.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class Pet(
    var id: String = "",
    var name: String = "",
    var type: String = "", // dog, cat, other
    var breed: String = "",
    var age: String = "",
    var gender: String = "",
    var description: String = "",
    var imageUrls: List<String> = emptyList(),
    var videoUrls: List<String> = emptyList(),
    var available: Boolean = true, // Main availability field - matches Firestore
    var location: String = "",
    var addedBy: String = "",
    var dateAdded: Long = System.currentTimeMillis(),
    var lastUpdated: Long = System.currentTimeMillis(),
    // Additional fields
    var personalityTraits: List<String> = emptyList(),
    var color: String = "",
    var vaccinated: Boolean = false,
    var ownerId: String = "",
    var specialNeeds: String = "",
    var createdAt: Timestamp? = null,
    var imageUrl: String = "",
    var adoptionFee: Double = 0.0,
    var contactNumber: String = "",
    var medicalHistory: String = "",
    var stability: String = "",
    var updatedAt: Timestamp? = null,
    var weight: Double = 0.0,
    var neutered: Boolean = false,
    var temperament: String = "",
    var shelterName: String = "",
    var category: String = "",
    var adopted: Boolean = false,
    var status: String = "available",
) {
    // No-argument constructor for Firebase
    constructor() : this(
        id = "",
        name = "",
        type = "",
        breed = "",
        age = "",
        gender = "",
        description = "",
        imageUrls = emptyList(),
        videoUrls = emptyList(),
        available = true,
        location = "",
        addedBy = "",
        dateAdded = System.currentTimeMillis(),
        lastUpdated = System.currentTimeMillis(),
    )

    // Single computed property to check availability
    @get:Exclude
    val isAvailable: Boolean
        get() = available && !adopted && status == "available"
}
