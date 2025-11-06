package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.syed.databinding.ActivitySignUpBinding
import com.syed.utils.ErrorHandler
import com.syed.utils.FirebaseUtils
import com.syed.utils.ValidationUtils
import java.util.Calendar
import java.util.Date

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private var isAgeVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        showAgeVerificationDialog()
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            signUpWithEmail()
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun signUpWithEmail() {
        val name =
            binding.etName.text
                .toString()
                .trim()
        val email =
            binding.etEmail.text
                .toString()
                .trim()
        val phone =
            binding.etPhone.text
                .toString()
                .trim()
        val password =
            binding.etPassword.text
                .toString()
                .trim()
        val confirmPassword =
            binding.etConfirmPassword.text
                .toString()
                .trim()

        // Check age verification
        if (!isAgeVerified) {
            Toast.makeText(this, "Please verify your age to continue", Toast.LENGTH_SHORT).show()
            showAgeVerificationDialog()
            return
        }

        // Use improved validation
        if (!isValidInput(name, email, phone, password, confirmPassword)) {
            return
        }

        binding.btnSignUp.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        FirebaseUtils.auth
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnSignUp.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE

                if (task.isSuccessful) {
                    val user = FirebaseUtils.auth.currentUser
                    user?.let {
                        // Use centralized user creation method
                        FirebaseUtils.createOrUpdateUserDocument { success ->
                            if (success) {
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                navigateToMain()
                            } else {
                                ErrorHandler.logError("SignUpActivity", "Failed to create user document")
                                navigateToMain() // Still navigate but log the issue
                            }
                        }
                    }
                } else {
                    ErrorHandler.handleAuthError(this, task.exception ?: Exception("Unknown auth error"))
                }
            }
    }

    private fun isValidInput(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
    ): Boolean {
        // Validate name
        if (!ValidationUtils.isValidName(name)) {
            binding.etName.error = "Please enter a valid name (letters only, at least 2 characters)"
            binding.etName.requestFocus()
            return false
        }

        // Validate email
        val emailError = ValidationUtils.getEmailValidationMessage(email)
        if (emailError != null) {
            binding.etEmail.error = emailError
            binding.etEmail.requestFocus()
            return false
        }

        // Validate phone
        if (!ValidationUtils.isValidPhone(phone)) {
            binding.etPhone.error = "Please enter a valid phone number"
            binding.etPhone.requestFocus()
            return false
        }

        // Validate password
        if (!ValidationUtils.isValidPassword(password)) {
            binding.etPassword.error = ValidationUtils.getPasswordStrengthMessage(password)
            binding.etPassword.requestFocus()
            return false
        }

        // Validate password confirmation
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            binding.etConfirmPassword.requestFocus()
            return false
        }

        return true
    }

    private fun navigateToMain() {
        startActivity(Intent(this, com.syed.MainActivity::class.java))
        finish()
    }

    private fun showAgeVerificationDialog() {
        val datePicker =
            MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Select your date of birth")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val birthDate = Date(selection)
            val age = calculateAge(birthDate)

            if (age < 13) {
                AlertDialog
                    .Builder(this)
                    .setTitle("Age Requirement")
                    .setMessage("You must be 13 or older to use this app.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }.setCancelable(false)
                    .show()
            } else {
                isAgeVerified = true
                Toast.makeText(this, "Age verified. You may proceed with registration.", Toast.LENGTH_SHORT).show()
            }
        }

        datePicker.addOnNegativeButtonClickListener {
            finish()
        }

        datePicker.addOnCancelListener {
            finish()
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun calculateAge(birthDate: Date): Int {
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance()
        birth.time = birthDate

        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }
}
