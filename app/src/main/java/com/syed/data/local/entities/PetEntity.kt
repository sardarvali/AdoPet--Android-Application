package com.syed.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val breed: String,
    val age: String,
    val gender: String,
    val description: String,
    val imageUrls: String, // JSON string
    val videoUrls: String, // JSON string
    val available: Boolean,
    val location: String,
    val addedBy: String,
    val dateAdded: Long,
    val lastUpdated: Long,
    val personalityTraits: String, // JSON string
    val color: String,
    val vaccinated: Boolean,
    val specialNeeds: String,
    val imageUrl: String,
    val adoptionFee: Double,
    val contactNumber: String,
    val medicalHistory: String,
    val weight: Double,
    val neutered: Boolean,
    val temperament: String,
    val shelterName: String,
    val adopted: Boolean,
    val status: String,
    val cachedAt: Long = System.currentTimeMillis(),
)
