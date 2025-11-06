package com.syed.utils

/**
 * Security constants and utilities for production deployment
 * Implements best practices for Google Play Store compliance
 */
object SecurityConstants {
    // API Rate limiting
    const val MAX_REQUESTS_PER_MINUTE = 20
    const val MAX_REQUESTS_PER_HOUR = 100

    // Image validation
    const val MAX_IMAGE_FILE_SIZE = 5 * 1024 * 1024 // 5MB
    const val MAX_IMAGE_DIMENSION = 2048 // pixels
    const val MIN_IMAGE_DIMENSION = 100 // pixels

    // Chat validation
    const val MAX_MESSAGE_LENGTH = 500
    const val MAX_CHAT_HISTORY = 50 // messages

    // Allowed image formats for security
    val ALLOWED_IMAGE_FORMATS = listOf("image/jpeg", "image/png", "image/webp")

    // Security headers for API requests
    val SECURITY_HEADERS =
        mapOf(
            "User-Agent" to "PetAdoptionAI/1.0",
            "Accept" to "application/json",
            "Cache-Control" to "no-cache",
        )

    // Timeout configurations
    const val API_TIMEOUT_SECONDS = 30
    const val IMAGE_UPLOAD_TIMEOUT_SECONDS = 60

    /**
     * Validates if a string contains potentially harmful content
     */
    fun isContentSafe(content: String): Boolean {
        val prohibitedPatterns =
            listOf(
                "<script",
                "</script>",
                "javascript:",
                "data:text/html",
                "vbscript:",
                "onload=",
                "onerror=",
                "onclick=",
            )

        val lowerContent = content.lowercase()
        return prohibitedPatterns.none { lowerContent.contains(it) }
    }

    /**
     * Sanitizes user input for API calls
     */
    fun sanitizeInput(input: String): String {
        return input
            .trim()
            .take(MAX_MESSAGE_LENGTH)
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
    }
}
