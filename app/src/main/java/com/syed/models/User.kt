package com.syed.models

data class User(
    var uid: String = "",
    var email: String = "",
    var name: String = "",
    var phone: String = "",
    var address: String = "",
    var profileImageUrl: String = "",
    var isAdmin: Boolean = false,
    var isActive: Boolean = true,
    var joinedDate: Long = System.currentTimeMillis(),
    var fcmToken: String = "", // Added missing FCM token field
    var lastLoginDate: Long = 0L, // Additional useful field
    var preferences: Map<String, Any> = emptyMap(), // User preferences
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", "", false, true, System.currentTimeMillis(), "", 0L, emptyMap())

    // Constructor with id parameter for compatibility
    constructor(
        id: String,
        email: String,
        name: String,
        phone: String,
        address: String,
        profileImageUrl: String,
        isAdmin: Boolean = false,
    ) : this(id, email, name, phone, address, profileImageUrl, isAdmin, true, System.currentTimeMillis(), "", 0L, emptyMap())
}
