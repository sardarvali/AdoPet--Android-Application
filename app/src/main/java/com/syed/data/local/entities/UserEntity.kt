package com.syed.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val name: String,
    val phone: String,
    val address: String,
    val profileImageUrl: String,
    val isAdmin: Boolean,
    val isActive: Boolean,
    val joinedDate: Long,
    val fcmToken: String,
    val lastLoginDate: Long,
    val preferences: String, // JSON string
    val cachedAt: Long = System.currentTimeMillis(),
)
