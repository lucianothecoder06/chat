package com.example.chat

import androidx.lifecycle.ViewModel
import com.example.chat.data.repository.AuthRepository

/** ViewModel de la pantalla principal provisional: muestra quién está logueado y cierra sesión. */
class MainViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    val currentEmail: String?
        get() = repository.currentEmail

    fun logout() {
        repository.logout()
    }
}
