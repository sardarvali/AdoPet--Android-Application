package com.syed.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.syed.R
import com.syed.databinding.ActivityLoginBinding
import com.syed.utils.FirebaseUtils
import com.syed.utils.ValidationUtils

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // Rate limiting for login attempts
    private var loginAttempts = 0
    private var lastLoginAttempt = 0L
    private val MAX_LOGIN_ATTEMPTS = 5
    private val RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000L // 15 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupClickListeners()

        // Check if user is already logged in
        if (FirebaseUtils.auth.currentUser != null) {
            navigateToMain()
        }
    }

    private fun setupGoogleSignIn() {
        try {
            val gso =
                GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            com.syed.utils.SecureLogger
                .e("LoginActivity", "Google Sign-in setup failed", e)
            // Hide Google Sign-in button if setup fails
            binding.btnGoogleSignIn.visibility = android.view.View.GONE
            Toast.makeText(this, "Google Sign-in not configured. Please use email login.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            loginWithEmail()
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            // Handle forgot password
            if (binding.etEmail.text
                    .toString()
                    .isNotEmpty()
            ) {
                resetPassword(binding.etEmail.text.toString())
            } else {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginWithEmail() {
        val email =
            binding.etEmail.text
                .toString()
                .trim()
        val password =
            binding.etPassword.text
                .toString()
                .trim()

        // Improved input validation
        if (!isValidInput(email, password)) {
            return
        }

        // Rate limiting check
        val now = System.currentTimeMillis()
        if (now - lastLoginAttempt > RATE_LIMIT_WINDOW_MS) {
            loginAttempts = 0 // Reset counter after time window
        }

        if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
            val remainingTime = RATE_LIMIT_WINDOW_MS - (now - lastLoginAttempt)
            val minutes = remainingTime / 60000
            Toast
                .makeText(
                    this,
                    "Too many failed attempts. Please try again in $minutes minutes.",
                    Toast.LENGTH_LONG,
                ).show()
            return
        }

        lastLoginAttempt = now

        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        FirebaseUtils.auth
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE

                if (task.isSuccessful) {
                    // Reset login attempts on success
                    loginAttempts = 0

                    // Ensure user document exists before navigating
                    FirebaseUtils.createOrUpdateUserDocument { success ->
                        if (success) {
                            navigateToMain()
                        } else {
                            com.syed.utils.SecureLogger
                                .w("LoginActivity", "Failed to create/update user document")
                            navigateToMain() // Still navigate but log the issue
                        }
                    }
                } else {
                    // Increment failed attempts
                    loginAttempts++

                    // Use generic error message (Security: Issue 1.2)
                    com.syed.utils.SecureLogger
                        .e("LoginActivity", "Login failed", task.exception ?: Exception("Unknown error"))
                    Toast
                        .makeText(
                            this,
                            "Login failed. Please check your credentials and try again.",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
    }

    private fun isValidInput(
        email: String,
        password: String,
    ): Boolean {
        val emailError = ValidationUtils.getEmailValidationMessage(email)
        if (emailError != null) {
            binding.etEmail.error = emailError
            binding.etEmail.requestFocus()
            return false
        }

        if (!ValidationUtils.isValidPassword(password)) {
            binding.etPassword.error = ValidationUtils.getPasswordStrengthMessage(password)
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                com.syed.utils.SecureLogger
                    .e("GoogleSignIn", "Sign-in failed", e)
                Toast.makeText(this, "Google sign in failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnGoogleSignIn.isEnabled = false

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseUtils.auth
            .signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnGoogleSignIn.isEnabled = true

                if (task.isSuccessful) {
                    val user = FirebaseUtils.auth.currentUser
                    user?.let {
                        // Use the centralized method with proper callback
                        FirebaseUtils.createOrUpdateUserDocument { success ->
                            if (success) {
                                navigateToMain()
                            } else {
                                com.syed.utils.SecureLogger
                                    .w("LoginActivity", "Failed to create/update user document")
                                navigateToMain() // Still navigate but log the issue
                            }
                        }
                    } ?: run {
                        com.syed.utils.SecureLogger
                            .e("LoginActivity", "Current user is null after Google sign-in")
                        Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    com.syed.utils.SecureLogger
                        .e("GoogleAuth", "Authentication failed", task.exception ?: Exception("Unknown error"))
                    Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun resetPassword(email: String) {
        FirebaseUtils.auth
            .sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, com.syed.MainActivity::class.java))
        finish()
    }
}
