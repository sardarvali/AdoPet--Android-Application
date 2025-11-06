package com.syed.models

data class RescueRequest(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var userEmail: String = "",
    var userPhone: String = "",
    var location: String = "",
    var description: String = "",
    var petType: String = "",
    var urgency: String = "", // low, medium, high
    var imageUrl: String = "", // Single image URL for compatibility
    var imageUrls: List<String> = emptyList(), // Multiple images
    var status: String = "pending", // pending, in_progress, completed, cancelled
    var adminMessage: String = "",
    var adminReply: String = "", // Admin's reply to the user
    var isReplied: Boolean = false, // Whether admin has replied
    var replyDate: Long = 0, // When admin replied
    var requestDate: Long = System.currentTimeMillis(),
    var responseDate: Long = 0,
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", "", "", "", "", "", "", "", emptyList(), "pending", "", "", false, 0, System.currentTimeMillis(), 0)
}
