package com.example.chat

import androidx.lifecycle.ViewModel
import com.example.chat.data.repository.AuthRepository

/**
 * ViewModel de la pantalla principal provisional: muestra quién está logueado,
 * guarda el token de notificaciones y cierra sesión.
 */
class MainViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    val currentEmail: String?
        get() = repository.currentEmail

    fun refreshFcmToken() {
        repository.refreshFcmToken()
    }

    fun logout() {
        repository.logout()
    }
}
