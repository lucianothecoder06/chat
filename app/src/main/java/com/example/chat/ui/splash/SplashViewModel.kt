package com.example.chat.ui.splash

import androidx.lifecycle.ViewModel
import com.example.chat.data.repository.AuthRepository

/** Decide la primera pantalla según si Firebase tiene una sesión guardada. */
class SplashViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
}
