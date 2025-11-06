package com.syed.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.syed.R
import com.syed.databinding.ActivityPetIdentificationBinding
import com.syed.services.AIApiService

/**
 * Secure Pet Identification Activity using ML Kit for on-device pet detection
 * Replaces Cloud Vision API with free, offline ML Kit Image Labeling
 *
 * Features:
 * - On-device pet detection (no network required)
 * - 100% free (no API costs)
 * - Instant results (<100ms)
 * - Works offline
 * - Root detection for security
 */
class PetIdentificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPetIdentificationBinding
    private lateinit var aiApiService: AIApiService
    private var identifiedPet: String? = null
    private var selectedImageBitmap: Bitmap? = null

    // Activity result launcher for ImagePicker
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                uri?.let { handleImageSelection(it) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetIdentificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize secure AI service
        aiApiService = AIApiService.getInstance(this)

        setupUI()
        setupClickListeners()

        // Check API configuration on startup
        checkApiConfiguration()
    }

    private fun setupUI() {
        // Set up toolbar if needed
        supportActionBar?.title = "AI Pet Identifier"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupClickListeners() {
        binding.btnCamera.setOnClickListener {
            openCamera()
        }

        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        binding.btnChat.setOnClickListener {
            identifiedPet?.let { pet ->
                val intent =
                    Intent(this, AIChatActivity::class.java).apply {
                        putExtra("PET_BREED", pet)
                    }
                startActivity(intent)
            }
        }
    }

    /**
     * Checks API configuration and shows status to user
     */
    private fun checkApiConfiguration() {
        aiApiService.testApiConfiguration { configStatus ->
            if (configStatus.contains("❌")) {
                showError("API Configuration Issue:\n$configStatus")
            }
        }
    }

    /**
     * Opens camera using secure ImagePicker library
     */
    private fun openCamera() {
        ImagePicker
            .with(this)
            .cameraOnly()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .saveDir(getFilesDir())
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }

    /**
     * Opens gallery using secure ImagePicker library
     */
    private fun openGallery() {
        ImagePicker
            .with(this)
            .galleryOnly()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }

    /**
     * Handles image selection from camera or gallery
     */
    private fun handleImageSelection(uri: Uri) {
        try {
            val bitmap = uriToBitmap(uri)

            // Security validation: Check image size
            if (bitmap.byteCount > 5 * 1024 * 1024) { // 5MB limit
                showError("Image too large. Please select a smaller image.")
                return
            }

            selectedImageBitmap = bitmap
            binding.imageView.setImageBitmap(bitmap)

            // Reset UI state
            resetResultUI()

            // Analyze with ML Kit (no Base64 conversion needed!)
            analyzeImage(bitmap)
        } catch (e: Exception) {
            showError("Failed to process image: ${e.message}")
        }
    }

    /**
     * Analyzes the selected image using ML Kit (on-device, FREE)
     */
    private fun analyzeImage(bitmap: Bitmap) {
        showProgressUI()

        aiApiService.identifyPet(
            bitmap = bitmap, // Direct bitmap, no Base64!
            onSuccess = { petName, confidence ->
                runOnUiThread {
                    hideProgressUI()
                    showResult(petName, confidence)
                }
            },
            onError = { error ->
                runOnUiThread {
                    hideProgressUI()
                    showError(error)
                }
            },
        )
    }

    /**
     * Shows analysis progress UI
     */
    private fun showProgressUI() {
        binding.progressCard.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE
    }

    /**
     * Hides progress UI
     */
    private fun hideProgressUI() {
        binding.progressCard.visibility = View.GONE
    }

    /**
     * Shows identification results
     */
    private fun showResult(
        petName: String,
        confidence: Float,
    ) {
        identifiedPet = petName
        val confidencePercentage = (confidence * 100).toInt()

        binding.tvResult.text = "$petName ($confidencePercentage% confidence)"
        binding.resultCard.visibility = View.VISIBLE
        binding.btnChat.visibility = View.VISIBLE
    }

    /**
     * Resets result UI
     */
    private fun resetResultUI() {
        binding.resultCard.visibility = View.GONE
        binding.btnChat.visibility = View.GONE
        identifiedPet = null
    }

    /**
     * Shows error message to user
     */
    private fun showError(message: String) {
        binding.resultCard.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Converts URI to Bitmap with proper error handling
     */
    private fun uriToBitmap(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up bitmap to prevent memory leaks
        selectedImageBitmap?.recycle()
        selectedImageBitmap = null
    }
}
