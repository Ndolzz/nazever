package com.naze.nazever.ui.auth

/**
 * Pure form validation rules for the auth screens (TASK-006).
 * Messages are fixed Indonesian strings; they never contain user input.
 */
object AuthFormValidator {

    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+([.][A-Za-z0-9-]+)+$")

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email tidak boleh kosong."
        !EMAIL_REGEX.matches(email.trim()) -> "Format email tidak valid."
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Kata sandi tidak boleh kosong."
        password.length < MIN_PASSWORD_LENGTH -> "Kata sandi minimal " + MIN_PASSWORD_LENGTH + " karakter."
        else -> null
    }

    fun validateConfirm(password: String, confirm: String): String? = when {
        confirm.isBlank() -> "Konfirmasi kata sandi tidak boleh kosong."
        confirm != password -> "Konfirmasi kata sandi tidak sama."
        else -> null
    }
}
