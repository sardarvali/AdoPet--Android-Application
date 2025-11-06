package com.syed.models

import com.google.firebase.Timestamp

data class ShelterRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val shelterName: String = "",
    val shelterType: String = "", // "ngo", "government", "private"
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val contactPerson: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val website: String = "",
    val images: List<String> = emptyList(),
    val capacity: Int = 0,
    val facilities: List<String> = emptyList(),
    val timings: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val adminNote: String = "",
    val dateSubmitted: Timestamp = Timestamp.now(),
    val dateProcessed: Timestamp? = null,
)
