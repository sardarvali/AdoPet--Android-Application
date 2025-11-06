package com.syed.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "adoption_requests")
data class AdoptionRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val petId: String,
    val status: String,
    val requestDate: Long,
    val cachedAt: Long = System.currentTimeMillis(),
)
