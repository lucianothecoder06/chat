package com.example.chat.util

import com.example.chat.R

/**
 * Validaciones de los formularios.
 * Cada función devuelve el id del mensaje de error (R.string...) o null si el dato es válido.
 * No usa android.util.Patterns porque esa clase no funciona en los tests JUnit locales.
 */
object Validators {

    const val MIN_PASSWORD_LENGTH = 6 // mínimo que acepta Firebase Auth
    const val MAX_NAME_LENGTH = 40

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): Int? {
        if (email.isBlank()) return R.string.error_email_empty
        if (!EMAIL_REGEX.matches(email.trim())) return R.string.error_email_invalid
        return null
    }

    fun validatePassword(password: String): Int? {
        if (password.isEmpty()) return R.string.error_password_empty
        if (password.length < MIN_PASSWORD_LENGTH) return R.string.error_password_short
        return null
    }

    fun validatePasswordConfirmation(password: String, confirmation: String): Int? {
        if (password != confirmation) return R.string.error_password_mismatch
        return null
    }

    fun validateName(name: String): Int? {
        if (name.isBlank()) return R.string.error_name_empty
        if (name.trim().length > MAX_NAME_LENGTH) return R.string.error_name_long
        return null
    }

    fun validateMessage(text: String): Int? {
        if (text.isBlank()) return R.string.error_message_empty
        return null
    }
}
