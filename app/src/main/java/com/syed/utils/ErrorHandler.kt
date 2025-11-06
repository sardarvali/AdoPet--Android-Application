package com.syed.utils

import android.content.Context
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

object ErrorHandler {
    fun handleFirebaseError(
        context: Context,
        exception: Exception,
        fallbackMessage: String,
    ) {
        val message =
            when (exception) {
                is FirebaseNetworkException -> "Network error. Please check your connection."
                is FirebaseFirestoreException -> {
                    when (exception.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Access denied. Please check your permissions."
                        FirebaseFirestoreException.Code.UNAVAILABLE -> "Service temporarily unavailable. Please try again."
                        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "Request timed out. Please try again."
                        else -> "Database error: ${exception.message}"
                    }
                }
                is FirebaseException -> "Firebase error: ${exception.message}"
                else -> fallbackMessage
            }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        android.util.Log.e("ErrorHandler", "Error occurred: ${exception.message}", exception)
    }

    fun isNetworkError(exception: Exception): Boolean =
        exception is FirebaseNetworkException ||
            (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.UNAVAILABLE)

    fun logError(
        tag: String,
        message: String,
        exception: Exception? = null,
    ) {
        android.util.Log.e(tag, message, exception)
    }

    fun handleAuthError(
        context: Context,
        exception: Exception,
    ) {
        val message =
            when (exception) {
                is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password is too weak"
                is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account with this email already exists"
                is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "User account not found"
                is FirebaseNetworkException -> "Network error. Please check your connection."
                else -> "Authentication failed: ${exception.message}"
            }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        logError("AuthError", "Authentication error occurred", exception)
    }
}
