package com.syed.models

data class ContactMessage(
    var id: String = "",
    var userId: String = "",
    var userName: String = "", // Added for constructor compatibility
    var userEmail: String = "", // Added for constructor compatibility
    var userPhone: String = "",
    var subject: String = "", // Added for constructor compatibility
    var message: String = "", // Added for constructor compatibility
    var status: String = "unread", // unread, read, replied
    var adminReply: String = "",
    var sentDate: Long = System.currentTimeMillis(),
    var replyDate: Long = 0,
    var isReplied: Boolean = false, // Standardized with 'is' prefix
    var repliedBy: String = "",
    var isRead: Boolean = false, // Standardized with 'is' prefix
    var timestamp: Long = System.currentTimeMillis(), // Added for Firebase compatibility
) {
    // No-argument constructor for Firebase
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "unread",
        "",
        System.currentTimeMillis(),
        0,
        false,
        "",
        false,
        System.currentTimeMillis(),
    )

    // Constructor with parameters that activities expect
    constructor(
        userName: String,
        userEmail: String,
        subject: String,
        message: String,
        userId: String = "",
        userPhone: String = "",
    ) : this(
        id = "",
        userId = userId,
        userName = userName,
        userEmail = userEmail,
        userPhone = userPhone,
        subject = subject,
        message = message,
        status = "unread",
        adminReply = "",
        sentDate = System.currentTimeMillis(),
        replyDate = 0,
        isReplied = false,
        repliedBy = "",
        isRead = false,
        timestamp = System.currentTimeMillis(),
    )
}
