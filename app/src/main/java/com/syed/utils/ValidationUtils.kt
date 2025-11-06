package com.syed.utils

object ValidationUtils {
    fun isValidName(name: String): Boolean = name.trim().length >= 2 && name.matches(Regex("^[a-zA-Z\\s]+$"))

    fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS
            .matcher(email)
            .matches()

    fun isValidPhone(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        return cleanPhone.matches(Regex("^[+]?[0-9]{10,15}$"))
    }

    fun isValidAge(age: String): Boolean =
        try {
            val ageInt = age.toInt()
            ageInt in 0..50 // Reasonable age range for pets
        } catch (e: NumberFormatException) {
            false
        }

    fun isValidPetName(name: String): Boolean = name.trim().length >= 1 && name.length <= 50

    fun isValidDescription(description: String): Boolean = description.trim().length >= 10 && description.length <= 1000

    fun isValidSubject(subject: String): Boolean = subject.trim().length >= 3 && subject.length <= 100

    fun isValidMessage(message: String): Boolean = message.trim().length >= 10 && message.length <= 2000

    fun validateAdoptionForm(
        name: String,
        email: String,
        phone: String,
        address: String,
        experience: String,
        reason: String,
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (!isValidName(name)) {
            errors.add("Please enter a valid name (letters only, at least 2 characters)")
        }

        if (!isValidEmail(email)) {
            errors.add("Please enter a valid email address")
        }

        if (!isValidPhone(phone)) {
            errors.add("Please enter a valid phone number")
        }

        if (address.trim().length < 10) {
            errors.add("Please provide a complete address (at least 10 characters)")
        }

        if (experience.trim().length < 20) {
            errors.add("Please describe your experience with pets (at least 20 characters)")
        }

        if (reason.trim().length < 20) {
            errors.add("Please explain why you want to adopt this pet (at least 20 characters)")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun getEmailValidationMessage(email: String): String? =
        if (!isValidEmail(email)) {
            when {
                email.trim().isEmpty() -> "Email is required"
                !email.contains("@") -> "Please enter a valid email address"
                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches() ->
                    "Please enter a valid email address"
                else -> "Invalid email format"
            }
        } else {
            null
        }

    fun isValidPassword(password: String): Boolean =
        password.length >= 6 &&
            password.any { it.isDigit() } &&
            password.any { it.isLetter() }

    fun getPasswordStrengthMessage(password: String): String? =
        when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            !password.any { it.isDigit() } -> "Password must contain at least one number"
            !password.any { it.isLetter() } -> "Password must contain at least one letter"
            else -> null
        }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
) {
    val message: String
        get() = errors.joinToString("\n")
}
