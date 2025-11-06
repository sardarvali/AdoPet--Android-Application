package com.syed.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Secure image processing utilities for AI pet identification
 * Implements security best practices for production use
 */
object ImageUtils {
    private const val TAG = "ImageUtils"
    private const val MAX_IMAGE_SIZE = 2048 // Max dimension for processing
    private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB max file size (matching Firebase rules)
    private const val COMPRESSION_QUALITY = 80 // JPEG compression quality

    /**
     * Validates image size before upload to prevent bandwidth waste
     */
    fun validateImageBeforeUpload(
        imageUri: Uri,
        context: Context,
    ): Boolean {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val fileSize = inputStream?.available() ?: 0
            inputStream?.close()

            if (fileSize > MAX_FILE_SIZE) {
                Toast.makeText(context, "Image too large. Max size: 10MB", Toast.LENGTH_SHORT).show()
                return false
            }

            return true
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Error validating image size", e)
            return false
        }
    }

    /**
     * Compress image before upload to reduce bandwidth and storage costs
     */
    fun compressImage(
        imageUri: Uri,
        context: Context,
    ): ByteArray? {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                SecureLogger.e(TAG, "Failed to decode bitmap")
                return null
            }

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            bitmap.recycle()

            return outputStream.toByteArray()
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Error compressing image", e)
            return null
        }
    }

    /**
     * Validates image before processing
     */
    fun validateImage(
        context: Context,
        uri: Uri,
    ): ValidationResult {
        try {
            val inputStream =
                context.contentResolver.openInputStream(uri)
                    ?: return ValidationResult.Error("Cannot open image file")

            // Check file size
            val fileSize = inputStream.available()
            if (fileSize > MAX_FILE_SIZE) {
                return ValidationResult.Error("Image too large (max 10MB)")
            }

            // Decode bitmap to check format
            val bitmap =
                BitmapFactory.decodeStream(inputStream)
                    ?: return ValidationResult.Error("Invalid image format")

            bitmap.recycle() // Clean up
            return ValidationResult.Success
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Error validating image", e)
            return ValidationResult.Error("Failed to validate image")
        }
    }

    /**
     * Safely resizes bitmap for AI processing
     */
    fun resizeBitmapForAI(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scaling factor
        val scaleFactor =
            when {
                width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE -> {
                    val maxDimension = maxOf(width, height)
                    MAX_IMAGE_SIZE.toFloat() / maxDimension
                }
                else -> 1.0f
            }

        return if (scaleFactor < 1.0f) {
            val newWidth = (width * scaleFactor).toInt()
            val newHeight = (height * scaleFactor).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * Corrects image orientation based on EXIF data
     */
    fun correctImageOrientation(
        context: Context,
        uri: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        try {
            val inputStream: InputStream =
                context.contentResolver.openInputStream(uri)
                    ?: return bitmap

            val exif = ExifInterface(inputStream)
            val orientation =
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Error correcting image orientation", e)
            return bitmap
        }
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        degrees: Float,
    ): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    sealed class ValidationResult {
        object Success : ValidationResult()

        data class Error(
            val message: String,
        ) : ValidationResult()
    }
}
