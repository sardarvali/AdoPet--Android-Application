package com.syed.chat

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * Media Upload Manager for Chat
 * Handles uploading images, videos, documents, and voice notes
 */
class ChatMediaUploadManager {
    private val storage = FirebaseStorage.getInstance()

    data class UploadResult(
        val success: Boolean,
        val downloadUrl: String = "",
        val thumbnailUrl: String = "",
        val fileSize: Long = 0,
        val fileName: String = "",
        val duration: Long = 0,
        val error: String? = null,
    )

    /**
     * Upload image with compression
     */
    suspend fun uploadImage(
        uri: Uri,
        context: Context,
    ): UploadResult =
        try {
            val storageRef = storage.reference
            val imageRef = storageRef.child("chat_media/images/${System.currentTimeMillis()}.jpg")

            // TODO: Add image compression here
            val uploadTask = imageRef.putFile(uri).await()
            val downloadUrl = imageRef.downloadUrl.await()

            UploadResult(
                success = true,
                downloadUrl = downloadUrl.toString(),
                fileSize = uploadTask.metadata?.sizeBytes ?: 0L,
            )
        } catch (e: Exception) {
            UploadResult(success = false, error = e.message)
        }

    /**
     * Upload video with thumbnail generation
     */
    suspend fun uploadVideo(
        uri: Uri,
        context: Context,
    ): UploadResult =
        try {
            val storageRef = storage.reference
            val videoRef = storageRef.child("chat_media/videos/${System.currentTimeMillis()}.mp4")

            val uploadTask = videoRef.putFile(uri).await()
            val downloadUrl = videoRef.downloadUrl.await()

            // TODO: Generate and upload thumbnail
            val thumbnailUrl = ""

            UploadResult(
                success = true,
                downloadUrl = downloadUrl.toString(),
                thumbnailUrl = thumbnailUrl,
                fileSize = uploadTask.metadata?.sizeBytes ?: 0L,
            )
        } catch (e: Exception) {
            UploadResult(success = false, error = e.message)
        }

    /**
     * Upload audio/voice note
     */
    suspend fun uploadAudio(filePath: String): UploadResult =
        try {
            val file = File(filePath)
            val storageRef = storage.reference
            val audioRef = storageRef.child("chat_media/audio/${System.currentTimeMillis()}.3gp")

            val uploadTask = audioRef.putFile(Uri.fromFile(file)).await()
            val downloadUrl = audioRef.downloadUrl.await()

            // TODO: Get audio duration
            val duration = 0L

            UploadResult(
                success = true,
                downloadUrl = downloadUrl.toString(),
                fileSize = uploadTask.metadata?.sizeBytes ?: 0L,
                duration = duration,
            )
        } catch (e: Exception) {
            UploadResult(success = false, error = e.message)
        }

    /**
     * Upload document
     */
    suspend fun uploadDocument(
        uri: Uri,
        context: Context,
    ): UploadResult =
        try {
            val fileName = getFileName(uri, context)
            val storageRef = storage.reference
            val docRef = storageRef.child("chat_media/documents/${System.currentTimeMillis()}_$fileName")

            val uploadTask = docRef.putFile(uri).await()
            val downloadUrl = docRef.downloadUrl.await()

            UploadResult(
                success = true,
                downloadUrl = downloadUrl.toString(),
                fileSize = uploadTask.metadata?.sizeBytes ?: 0L,
                fileName = fileName,
            )
        } catch (e: Exception) {
            UploadResult(success = false, error = e.message)
        }

    private fun getFileName(
        uri: Uri,
        context: Context,
    ): String {
        var fileName = "document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}
