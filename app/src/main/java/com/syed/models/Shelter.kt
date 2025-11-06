package com.syed.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Shelter(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "ngo", "government", "private"
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val location: GeoPoint? = null, // latitude, longitude
    val contactPerson: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val website: String = "",
    val images: List<String> = emptyList(),
    val capacity: Int = 0,
    val currentOccupancy: Int = 0,
    val facilities: List<String> = emptyList(), // ["veterinary", "food", "shelter", "adoption"]
    val timings: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val verified: Boolean = false, // Whether shelter is verified by admin
    val addedBy: String = "", // userId
    val addedByType: String = "", // "admin", "user"
    val dateAdded: Timestamp = Timestamp.now(),
    val dateModified: Timestamp = Timestamp.now(),
)
