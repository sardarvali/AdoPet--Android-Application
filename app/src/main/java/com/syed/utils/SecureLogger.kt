package com.syed.utils

import android.util.Log
import com.syed.BuildConfig

/**
 * Secure logging utility that only logs in debug builds
 * Prevents sensitive information leakage in production
 */
object SecureLogger {
    /**
     * Debug level log - only in debug builds
     */
    fun d(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * Debug level log with throwable - only in debug builds
     */
    fun d(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message, throwable)
        }
    }

    /**
     * Info level log - only in debug builds
     */
    fun i(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    /**
     * Verbose level log - only in debug builds
     */
    fun v(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    /**
     * Warning level log - logs in all builds but sanitizes sensitive data
     */
    fun w(
        tag: String,
        message: String,
    ) {
        Log.w(tag, sanitize(message))
    }

    /**
     * Warning level log with throwable
     */
    fun w(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        Log.w(tag, sanitize(message), throwable)
    }

    /**
     * Error level log - logs in all builds but sanitizes sensitive data
     */
    fun e(
        tag: String,
        message: String,
    ) {
        Log.e(tag, sanitize(message))
    }

    /**
     * Error level log with throwable
     */
    fun e(
        tag: String,
        message: String,
        throwable: Throwable,
    ) {
        Log.e(tag, sanitize(message), throwable)
    }

    /**
     * Sanitize sensitive data from log messages
     * Removes API keys, tokens, emails, and other sensitive patterns
     */
    private fun sanitize(message: String): String {
        if (BuildConfig.DEBUG) {
            return message // Don't sanitize in debug builds
        }

        return message
            // Mask email addresses
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "***@***.***")
            // Mask potential API keys (long alphanumeric strings)
            .replace(Regex("[A-Za-z0-9]{32,}"), "***REDACTED***")
            // Mask Firebase UIDs
            .replace(Regex("uid=\\S+"), "uid=***")
            // Mask tokens
            .replace(Regex("token=\\S+"), "token=***")
    }

    /**
     * Log sensitive data - only in debug builds, with clear marking
     */
    fun sensitive(
        tag: String,
        message: String,
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, "[SENSITIVE] $message")
        }
    }
}
